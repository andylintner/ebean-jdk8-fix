package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.Attribute;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;
/**
 * Used to collect information about a field (specifically from field annotations).
 */
public class LocalFieldVisitor extends FieldVisitor {

	private final FieldMeta fieldMeta;
	
	/**
	 * Constructor used for agent class enhancement.
   *
	 * @param fv the fieldVisitor used to write
	 * @param fieldMeta the fieldMeta data
	 */
	public LocalFieldVisitor(FieldVisitor fv, FieldMeta fieldMeta) {
    super(Opcodes.ASM5, fv);
		this.fieldMeta = fieldMeta;
	}

	/**
	 * Return the field name.
	 */
	public String getName() {
		return fieldMeta.getFieldName();
	}
	
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		fieldMeta.addAnnotationDesc(desc);
		if (fv != null){
			return fv.visitAnnotation(desc, visible);			
		} else {
			return null;
		}
	}

	public void visitAttribute(Attribute attr) {
		if (fv != null){
			fv.visitAttribute(attr);
		}
	}

	public void visitEnd() {
		if (fv != null){
			fv.visitEnd();
		}
	}

}
