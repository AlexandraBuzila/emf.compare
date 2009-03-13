/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.emf.compare.epatch.tests.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;


/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class EmfFormatter {

	private static final char SPACE = ' ';
	private static final String INDENT = "    ";

	public static String objToStr(Object obj,
			EStructuralFeature... ignoredFeatures) {
		Appendable buf = new StringBuilder(1024);
		Set<EStructuralFeature> ignoreUs = (ignoredFeatures != null && ignoredFeatures.length != 0) ? (ignoredFeatures.length > 1 ? new HashSet<EStructuralFeature>(
				Arrays.asList(ignoredFeatures))
				: Collections.singleton(ignoredFeatures[0]))
				: Collections.<EStructuralFeature> emptySet();
		try {
			objToStrImpl(obj, "", buf, ignoreUs);
		} catch (Exception e) {
			throw new WrappedException(e);
		}
		return buf.toString();
	}

	private static void objToStrImpl(Object obj, String indent, Appendable buf,
			Set<EStructuralFeature> ignoreUs) throws Exception {
		String innerIdent = INDENT + indent;
		if (obj instanceof EObject) {
			EObject eobj = (EObject) obj;
			buf.append(eobj.eClass().getName()).append(" {\n");
			for (EStructuralFeature f : eobj.eClass()
					.getEAllStructuralFeatures()) {
				if (!eobj.eIsSet(f) || ignoreUs.contains(f))
					continue;
				buf.append(innerIdent);
				if (f instanceof EReference) {
					EReference r = (EReference) f;
					if (r.isContainment()) {
						buf.append("cref ");
						buf.append(f.getEType().getName()).append(SPACE);
						buf.append(f.getName()).append(SPACE);
						objToStrImpl(eobj.eGet(f), innerIdent, buf, ignoreUs);
					} else {
						buf.append("ref ");
						buf.append(f.getEType().getName()).append(SPACE);
						buf.append(f.getName()).append(SPACE);
						refToStr(eobj, r, innerIdent, buf);
					}
				} else if (f instanceof EAttribute) {
					buf.append("attr ");
					buf.append(f.getEType().getName()).append(SPACE);
					buf.append(f.getName()).append(SPACE);
					// logger.debug(Msg.create("Path:").path(eobj));
					Object at = eobj.eGet(f);
					if (eobj != at)
						objToStrImpl(at, innerIdent, buf, ignoreUs);
					else
						buf.append("<same as container object>");
				} else {
					buf.append("attr ");
					buf.append(f.getEType().getName()).append(SPACE);
					buf.append(f.getName()).append(" ??????");
				}
				buf.append('\n');
			}
			buf.append(indent).append("}");
			return;
		}
		if (obj instanceof Collection) {
			int counter = 0;
			Collection<?> coll = (Collection<?>) obj;
			buf.append("[\n");
			for (Object o : coll) {
				buf.append(innerIdent);
				printInt(counter++, coll.size(), buf);
				buf.append(": ");
				objToStrImpl(o, innerIdent, buf, ignoreUs);
				buf.append("\n");
			}
			buf.append(indent + "]");
			return;
		}
		if (obj != null) {
			buf.append("'" + obj + "'");
			return;
		}
		buf.append("null");
	}

	@SuppressWarnings("unchecked")
	private static void refToStr(EObject obj, EReference ref, String indent,
			Appendable buf) throws Exception {
		Object o = obj.eGet(ref);
		if (o instanceof EObject) {
			EObject eo = (EObject) o;

			if (eo instanceof ENamedElement)
				buf.append("'").append(((ENamedElement) eo).getName()).append(
						"' ");
			buf.append("ref: ");
			getURI(obj, eo, buf);
			return;
		}
		if (o instanceof Collection) {
			String innerIndent = indent + INDENT;
			buf.append("[");
			int counter = 0;
			Collection coll = (Collection) o;
			for (Iterator i = coll.iterator(); i.hasNext();) {
				Object item = (Object) i.next();
				if (counter == 0)
					buf.append('\n');
				buf.append(innerIndent);
				printInt(counter++, coll.size(), buf);
				buf.append(": ");
				getURI(obj, (EObject) item, buf);
				if (i.hasNext())
					buf.append(",\n");
				else
					buf.append('\n').append(indent);
			}
			buf.append("]");
			return;
		}
		buf.append("?????");
	}

	private static void printInt(int current, int max, Appendable buffer)
			throws IOException {
		int length = getNumberOfDigits(current);
		int maxLength = getNumberOfDigits(max);
		buffer.append(Integer.toString(current));
		for (int i = maxLength; i > length; i--) {
			buffer.append(SPACE);
		}
	}

	private static int getNumberOfDigits(int number) {
		return number <= 1 ? 1 : (int) Math.floor(Math.log10(number + 0.5)) + 1;
	}

	private static void getURI(EObject parent, EObject target, Appendable buf)
			throws Exception {
		Resource r = target.eResource();
		if (r == null)
			buf.append("(resource null)");
		else if (parent.eResource() == r)
			buf.append(r.getURIFragment(target));
		else
			buf.append(r.getURI().toString()).append(r.getURIFragment(target));
	}
}
