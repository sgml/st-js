/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
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
package org.stjs.generator.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import org.stjs.generator.utils.AnnotationUtils;

import com.google.common.base.Preconditions;

/**
 * 
 * This is a wrapper around a method, but with the correct type for a generic type for example
 * 
 * @author acraciun
 * 
 */
@Immutable
public final class MethodWrapper {
	private final Method method;
	private final TypeWrapper returnType;
	private final TypeWrapper[] parameterTypes;
	private final TypeVariableWrapper<Method>[] typeParameters;
	private final int modifiers;
	private final TypeWrapper ownerType;
	private final boolean declared;

	public MethodWrapper(Method method, TypeWrapper returnType, TypeWrapper[] parameterTypes, int modifiers,
			TypeVariableWrapper<Method>[] typeParameters, TypeWrapper ownerType, boolean declared) {
		Preconditions.checkNotNull(method);
		Preconditions.checkNotNull(returnType);
		Preconditions.checkNotNull(parameterTypes);
		Preconditions.checkNotNull(typeParameters);
		// Preconditions.checkNotNull(ownerType);

		this.method = method;
		this.returnType = returnType;
		this.parameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length);
		this.modifiers = modifiers;
		this.ownerType = ownerType;
		this.declared = declared;
		this.typeParameters = Arrays.copyOf(typeParameters, typeParameters.length);
	}

	public String getName() {
		return method.getName();
	}

	public TypeWrapper getReturnType() {
		return returnType;
	}

	public TypeWrapper[] getParameterTypes() {
		return Arrays.copyOf(parameterTypes, parameterTypes.length);
	}

	public int getModifiers() {
		return modifiers;
	}

	public TypeWrapper getOwnerType() {
		return ownerType;
	}

	public TypeVariableWrapper<Method>[] getTypeParameters() {
		return Arrays.copyOf(typeParameters, typeParameters.length);
	}

	/**
	 * 
	 * @return true if the method was declared in the owner class, false if it was inherited
	 */
	public boolean isDeclared() {
		return declared;
	}

	public TypeWrapper getVarargParamType() {
		return parameterTypes.length > 0 ? parameterTypes[parameterTypes.length - 1].getComponentType() : null;
	}

	public boolean isCompatibleParameterTypes(TypeWrapper[] paramTypes) {
		int i = 0;
		for (i = 0; i < paramTypes.length && i < parameterTypes.length; ++i) {
			if (!parameterTypes[i].isAssignableFrom(paramTypes[i])) {
				break;
			}
		}
		if (i == paramTypes.length) {
			return true;
		}
		TypeWrapper varArgParamType = getVarargParamType();
		if (varArgParamType == null) {
			return false;
		}
		// try a varargs match
		i = parameterTypes.length - 1;
		for (; i < paramTypes.length; ++i) {
			if (!varArgParamType.isAssignableFrom(paramTypes[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(returnType).append(" ").append(getName()).append(" (");
		s.append(Arrays.toString(parameterTypes));
		s.append(")");
		return s.toString();
	}

	public boolean hasCompatibleNumberOfParams(int length) {
		if (parameterTypes.length == length) {
			return true;
		}
		if (length >= parameterTypes.length - 1 && parameterTypes.length > 0
				&& parameterTypes[parameterTypes.length - 1].getComponentType() != null) {
			// last param is a vararg
			return true;
		}
		return false;
	}

	public MethodWrapper withReturnType(TypeWrapper newReturnType) {
		return new MethodWrapper(method, newReturnType, parameterTypes, modifiers, typeParameters, ownerType, declared);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return AnnotationUtils.getAnnotation(ownerType.getType(), method, annotationClass);
	}
}
