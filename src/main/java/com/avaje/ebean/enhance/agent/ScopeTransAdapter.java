package com.avaje.ebean.enhance.agent;

import java.util.ArrayList;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;
import com.avaje.ebean.enhance.asm.Type;
import com.avaje.ebean.enhance.asm.commons.FinallyAdapter;

/**
 * Adapts a method to support Transactional.
 * <p>
 * Adds a TxScope and ScopeTrans local variables. On normal exit makes a call
 * out via InternalServer to end the scopeTrans depending on the exit type
 * opcode (ATHROW vs ARETURN etc) and whether particular throwable's cause a
 * rollback or not.
 * </p>
 */
public class ScopeTransAdapter extends FinallyAdapter implements EnhanceConstants {


	private static final Type txScopeType = Type.getType("L"+C_TXSCOPE+";");
	private static final Type scopeTransType = Type.getType(L_SCOPETRANS);
	private static final Type helpScopeTrans = Type.getType(L_HELPSCOPETRANS);

	private final AnnotationInfo annotationInfo;

	private final ClassAdapterTransactional owner;

  private final String methodName;

	private boolean transactional;

	private int posTxScope;
	private int posScopeTrans;
	
	public ScopeTransAdapter(ClassAdapterTransactional owner, final MethodVisitor mv, final int access, final String name, final String desc) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.owner = owner;
		this.methodName = name;
		
		// inherit from class level Transactional annotation
		AnnotationInfo parentInfo = owner.getClassAnnotationInfo();
		
		// inherit from interface method transactional annotation
		AnnotationInfo interfaceInfo = owner.getInterfaceTransactionalInfo(name, desc);
		if (parentInfo == null){
			parentInfo = interfaceInfo;
		} else {
			parentInfo.setParent(interfaceInfo);
		}
		
		// inherit transactional annotations from parentInfo
		annotationInfo = new AnnotationInfo(parentInfo);
		
		// default based on whether Transactional annotation
		// is at the class level or on interface method
		transactional = parentInfo != null; 
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(EnhanceConstants.AVAJE_TRANSACTIONAL_ANNOTATION)) {
			transactional = true;
		}
		AnnotationVisitor av = super.visitAnnotation(desc, visible);
		return new AnnotationInfoVisitor(null, annotationInfo, av);
	}

	private void setTxType(Object txType){
		
		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitLdcInsn(txType.toString());
		mv.visitMethodInsn(INVOKESTATIC, C_TXTYPE, "valueOf", "(Ljava/lang/String;)L"+C_TXTYPE+";", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, C_TXSCOPE, "setType", "(L"+C_TXTYPE+";)L"+C_TXSCOPE+";", false);
		mv.visitInsn(POP);
	}
	
	private void setTxIsolation(Object txIsolation){
		
		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitLdcInsn(txIsolation.toString());
		mv.visitMethodInsn(INVOKESTATIC, C_TXISOLATION, "valueOf", "(Ljava/lang/String;)L"+C_TXISOLATION+";", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, C_TXSCOPE, "setIsolation", "(L"+C_TXISOLATION+";)L"+C_TXSCOPE+";", false);
		mv.visitInsn(POP);
	}
	
	private void setServerName(Object serverName){
		
		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitLdcInsn(serverName.toString());
		mv.visitMethodInsn(INVOKEVIRTUAL, C_TXSCOPE, "setServerName", "(Ljava/lang/String;)L"+C_TXSCOPE+";", false);
		mv.visitInsn(POP);
	}
	
	private void setReadOnly(Object readOnlyObj){

		boolean readOnly = (Boolean)readOnlyObj;
		mv.visitVarInsn(ALOAD, posTxScope);
		if (readOnly){
			mv.visitInsn(ICONST_1);
		} else {
			mv.visitInsn(ICONST_0);
		}
		mv.visitMethodInsn(INVOKEVIRTUAL, C_TXSCOPE, "setReadOnly", "(Z)L"+C_TXSCOPE+";", false);
	}
	
	/**
	 * Add bytecode to add the noRollbackFor throwable types to the TxScope.
	 */
	private void setNoRollbackFor(Object noRollbackFor){

		ArrayList<?> list = (ArrayList<?>)noRollbackFor;
		
		for (int i = 0; i < list.size(); i++) {
			
			Type throwType =  (Type)list.get(i);
			
			mv.visitVarInsn(ALOAD, posTxScope);
			mv.visitLdcInsn(throwType);
			mv.visitMethodInsn(INVOKEVIRTUAL, txScopeType.getInternalName(), "setNoRollbackFor", "(Ljava/lang/Class;)L"+C_TXSCOPE+";", false);
			mv.visitInsn(POP);
		}
	}
	
	/**
	 * Add bytecode to add the rollbackFor throwable types to the TxScope.
	 */
	private void setRollbackFor(Object rollbackFor){

		ArrayList<?> list = (ArrayList<?>)rollbackFor;
		
		for (int i = 0; i < list.size(); i++) {
			
			Type throwType =  (Type)list.get(i);
			
			mv.visitVarInsn(ALOAD, posTxScope);
			mv.visitLdcInsn(throwType);
			mv.visitMethodInsn(INVOKEVIRTUAL, txScopeType.getInternalName(), "setRollbackFor", "(Ljava/lang/Class;)L"+C_TXSCOPE+";", false);
			mv.visitInsn(POP);
		}
	}
	
	@Override
	protected void onMethodEnter() {

		if (!transactional) {
			return;
		}
		
		// call back to owner to log debug information
		owner.transactionalMethod(methodName, methodDesc, annotationInfo);

		posTxScope = newLocal(txScopeType);
		posScopeTrans = newLocal(scopeTransType);

		mv.visitTypeInsn(NEW, txScopeType.getInternalName());
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, txScopeType.getInternalName(), "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, posTxScope);
		
		Object txType = annotationInfo.getValue("type");
		if (txType != null){
			setTxType(txType);
		}
		
		Object txIsolation = annotationInfo.getValue("isolation");
		if (txIsolation != null){
			setTxIsolation(txIsolation);
		}
		
		Object readOnly = annotationInfo.getValue("readOnly");
		if (readOnly != null){
			setReadOnly(readOnly);
		}
		
		Object noRollbackFor = annotationInfo.getValue("noRollbackFor");
		if (noRollbackFor != null){
			setNoRollbackFor(noRollbackFor);
		}
		
		Object rollbackFor = annotationInfo.getValue("rollbackFor");
		if (rollbackFor != null){
			setRollbackFor(rollbackFor);
		}

		Object serverName = annotationInfo.getValue("serverName");
		if (serverName != null && !serverName.equals("")){
			setServerName(serverName);
		}

		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitMethodInsn(INVOKESTATIC, helpScopeTrans.getInternalName(), "createScopeTrans", "("
				+ txScopeType.getDescriptor() + ")" + scopeTransType.getDescriptor(), false);
		mv.visitVarInsn(ASTORE, posScopeTrans);
	}


	@Override
  protected void onFinally(int opcode) {
	  onExit(opcode);
  }

  protected void onExit(int opcode) {

		if (!transactional) {
			return;
		}

		if (opcode == RETURN) {
			visitInsn(ACONST_NULL);

		} else if (opcode == ARETURN || opcode == ATHROW) {
			dup();

		} else {
			if (opcode == LRETURN || opcode == DRETURN) {
				dup2();
			} else {
				dup();
			}
			box(Type.getReturnType(this.methodDesc));
		}
		visitIntInsn(SIPUSH, opcode);
		loadLocal(posScopeTrans);

		visitMethodInsn(INVOKESTATIC, helpScopeTrans.getInternalName(), "onExitScopeTrans", "(Ljava/lang/Object;I"
				+ scopeTransType.getDescriptor() + ")V", false);
	}

}
