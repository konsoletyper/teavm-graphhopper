/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.graphhopper;

import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Helper;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.util.ModelUtils;

/**
 *
 * @author Alexey Andreev
 */
class GraphHopperPatcher implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getName().equals(Helper.class.getName())) {
            patchHelper(cls, innerSource);
        } else if (cls.getName().equals(EncodingManager.class.getName())) {
            patchEncodingManager(cls);
        }
    }

    private void patchHelper(ClassHolder cls, ClassReaderSource innerSource) {
        MethodDescriptor desc = new MethodDescriptor("nf", long.class, String.class);
        MethodHolder nfMethod = cls.getMethod(desc);
        cls.removeMethod(nfMethod);

        ClassReader patchClass = innerSource.get(GraphHopperPatchMethods.class.getName());
        nfMethod = ModelUtils.copyMethod(patchClass.getMethod(desc));
        cls.addMethod(nfMethod);
    }

    private void patchEncodingManager(ClassHolder cls) {
        MethodHolder method = cls.getMethod(new MethodDescriptor("registerEncoder", AbstractFlagEncoder.class,
                void.class));
        Program program = method.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (int j = 0; j < block.getInstructions().size(); ++j) {
                Instruction insn = block.getInstructions().get(j);
                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction)insn;
                if (invoke.getMethod().getClassName().equals(String.class.getName()) &&
                        invoke.getMethod().getName().equals("format")) {
                    StringConstantInstruction constInsn = new StringConstantInstruction();
                    constInsn.setReceiver(invoke.getReceiver());
                    constInsn.setConstant("");
                    constInsn.setLocation(invoke.getLocation());
                    block.getInstructions().set(j, constInsn);
                }
            }
        }
    }
}
