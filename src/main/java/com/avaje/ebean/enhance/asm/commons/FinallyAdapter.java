package com.avaje.ebean.enhance.asm.commons;

import com.avaje.ebean.enhance.asm.Label;
import com.avaje.ebean.enhance.asm.MethodVisitor;

public abstract class FinallyAdapter extends AdviceAdapter {

	protected Label startFinally = new Label();

	public FinallyAdapter(final int api, MethodVisitor mv, int acc, String name, String desc) {
		super(api, mv, acc, name, desc);
	}

	public void visitCode() {
		super.visitCode();
		mv.visitLabel(startFinally);
	}

	public void visitMaxs(int maxStack, int maxLocals) {

		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		onFinally(ATHROW);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack, maxLocals);
	}

	protected void onMethodExit(int opcode) {
		if (opcode != ATHROW) {
			onFinally(opcode);
		}
	}

	protected abstract void onFinally(int opcode);
	

}
