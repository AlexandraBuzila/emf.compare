/*******************************************************************************
 * Copyright (c) 2006, 2007 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * This is a factory for an ecore metamodel. There is a factory by package. Each factory is used to create
 * instances of classifiers.
 * 
 * @author Cedric Brun <a href="mailto:cedric.brun@obeo.fr">cedric.brun@obeo.fr</a>
 */
public class EFactory {
	/** Ecore factory. */
	public static final EcoreFactory ECORE = EcorePackageImpl.init().getEcoreFactory();

	/** This {@link EFactory}'s shared isntance. */
	protected Object factoryImpl;

	/** Identifier of the factory. */
	protected String id = new String();

	/** The class loader for this {@link EcoreFactory}. */
	protected ClassLoader loader;

	/**
	 * Creates an {@link EFactory} from an {@link EPackage}.
	 * 
	 * @param factoryId
	 *            Identifier for the newly created {@link EFactory}.
	 * @param ePackage
	 *            The {@link EPackage} to create the factory for.
	 * @param classLoader
	 *            {@link ClassLoader} for this {@link EFactory}.
	 */
	public EFactory(String factoryId, EPackage ePackage, ClassLoader classLoader) {
		factoryImpl = ePackage.getEFactoryInstance();
		id = factoryId;
		loader = classLoader;
	}

	/**
	 * Instantiates an {@link EFactory} given the shortName of the factory to wrap.
	 * <p>
	 * The following creates an instance of the factory java.resources.ResourcesFactory :
	 * 
	 * <pre>
	 * new EFactory(&quot;java.resources&quot;, &quot;Resources&quot;, new ClassLoader());
	 * </pre>
	 * 
	 * @param factoryId
	 *            Identifier for the newly created {@link EFactory}.
	 * @param factoryShortName
	 *            is the factory short name
	 * @param classLoader
	 *            {@link ClassLoader} for this {@link EFactory}.
	 * @throws FactoryException
	 *             Thrown if the initialization fails somehow.
	 */
	public EFactory(String factoryId, String factoryShortName, ClassLoader classLoader)
			throws FactoryException {
		loader = classLoader;
		init(factoryId, factoryShortName, loader);
	}

	/**
	 * Returns the Identifier of this factory.
	 * 
	 * @return The Identifier of this factory.
	 */
	protected String getId() {
		return id;
	}

	private void init(String factoryId, String factoryShortName, ClassLoader classLoader)
			throws FactoryException {
		if (factoryId != null && factoryShortName != null && factoryId.length() > 0
				&& factoryShortName.length() > 0) {
			final String factoryError = "Factory error : "; //$NON-NLS-1$
			// Class name
			final String rPackageImplClassName = factoryId + "." + factoryShortName + "Package"; //$NON-NLS-1$ //$NON-NLS-2$
			// Class loader
			try {
				// Class
				final Class rPackageImplClass = Class.forName(rPackageImplClassName, true, classLoader);
				// Method
				final Field rPackageImplField = rPackageImplClass.getField("eINSTANCE"); //$NON-NLS-1$
				final Method rPackageImplGetRessourcesFactoryMethod = rPackageImplClass.getMethod("get" //$NON-NLS-1$
						+ factoryShortName + "Factory", new Class[] {}); //$NON-NLS-1$
				// Instances
				final Object packageImpl = rPackageImplField.get(null);
				factoryImpl = rPackageImplGetRessourcesFactoryMethod.invoke(packageImpl, new Object[] {});
				id = factoryId;
			} catch (ClassNotFoundException e) {
				throw new FactoryException(factoryError + e.getMessage());
			} catch (NoSuchFieldException e) {
				throw new FactoryException(factoryError + e.getMessage());
			} catch (NoSuchMethodException e) {
				throw new FactoryException(factoryError + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new FactoryException(factoryError + e.getMessage());
			} catch (InvocationTargetException e) {
				throw new FactoryException(factoryError + e.getMessage());
			}
		} else {
			throw new FactoryException("Factory not found : " + factoryId + ".impl." + factoryShortName //$NON-NLS-1$ //$NON-NLS-2$
					+ "FactoryImpl"); //$NON-NLS-1$
		}
	}

	private static Object eCall(Object object, String name, Object arg) throws FactoryException {
		try {
			Class[] methodParams = new Class[0];
			Object[] invocationParams = new Object[0];
			if (arg != null) {
				methodParams = new Class[] {arg.getClass()};
				invocationParams = new Object[] {arg};
			}
			final Method method = object.getClass().getMethod(name, methodParams);
			return method.invoke(object, invocationParams);
		} catch (NoSuchMethodException e) {
			throw new FactoryException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new FactoryException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new FactoryException(e.getMessage());
		}
	}

	/**
	 * Sets the value of the given feature of the object to the new value.
	 * 
	 * @param object
	 *            Object on which we want to set the feature value.
	 * @param name
	 *            The name of the feature to set.
	 * @param arg
	 *            New value to affect to the feature.
	 * @throws FactoryException
	 *             Thrown if the affectation fails.
	 */
	public static void eSet(EObject object, String name, Object arg) throws FactoryException {
		final EStructuralFeature feature = eStructuralFeature(object, name);
		if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
			try {
				final Class c = Class.forName(ETools.getEClassifierPath(feature.getEType()));
				final Method m = c.getMethod("get", new Class[] {String.class}); //$NON-NLS-1$
				final Object value = m.invoke(c, new Object[] {arg});
				object.eSet(feature, value);
			} catch (ClassNotFoundException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (NoSuchMethodException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (IllegalAccessException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (InvocationTargetException e) {
				EMFComparePlugin.getDefault().log(e, false);
			}
		} else {
			object.eSet(feature, arg);
		}
	}

	/**
	 * Sets the value of the given feature of the object to the new value.
	 * 
	 * @param object
	 *            Object on which we want to set the feature value.
	 * @param name
	 *            The name of the feature to set.
	 * @param arg
	 *            New value to affect to the feature.
	 * @param classLoader
	 *            Specific {@link ClassLoader} used to set the value.
	 * @throws FactoryException
	 *             Thrown if the affectation fails.
	 */
	public static void eSet(EObject object, String name, Object arg, ClassLoader classLoader)
			throws FactoryException {
		final EStructuralFeature feature = eStructuralFeature(object, name);
		if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
			try {
				final Class c = classLoader.loadClass(ETools.getEClassifierPath(feature.getEType()));
				final Method m = c.getMethod("get", new Class[] {String.class}); //$NON-NLS-1$
				final Object value = m.invoke(c, new Object[] {arg});
				object.eSet(feature, value);
			} catch (ClassNotFoundException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (NoSuchMethodException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (IllegalAccessException e) {
				EMFComparePlugin.getDefault().log(e, false);
			} catch (InvocationTargetException e) {
				EMFComparePlugin.getDefault().log(e, false);
			}
		} else {
			object.eSet(feature, arg);
		}
	}

	/**
	 * Adds the new value of the given feature of the object. If the structural feature isn't a list, it
	 * behaves like eSet.
	 * 
	 * @param object
	 *            Object on which we want to add to the feature values list.
	 * @param name
	 *            The name of the feature to add to.
	 * @param arg
	 *            New value to add to the feature values.
	 * @throws FactoryException
	 *             Thrown if the affectation fails.
	 */
	@SuppressWarnings("unchecked")
	public static void eAdd(EObject object, String name, Object arg) throws FactoryException {
		final Object list = object.eGet(eStructuralFeature(object, name));
		if (list != null && list instanceof List) {
			if (arg != null) {
				((List)list).add(arg);
			}
		} else {
			eSet(object, name, arg);
		}
	}

	/**
	 * Removes the value of the given feature of the object. If the structural feature isn't a list, it
	 * behaves like eSet(object, name, null).
	 * 
	 * @param object
	 *            Object on which we want to remove from the feature values list.
	 * @param name
	 *            The name of the feature to remove from.
	 * @param arg
	 *            Value to remove from the feature values, can be <code>null</code>.
	 * @throws FactoryException
	 *             Thrown if the removal fails.
	 */
	public static void eRemove(EObject object, String name, Object arg) throws FactoryException {
		final Object list = object.eGet(eStructuralFeature(object, name));
		if (list != null && list instanceof List) {
			if (arg != null) {
				((List)list).remove(arg);
			}
		} else {
			eSet(object, name, null);
		}
	}

	/**
	 * Gets the value of the given feature of the object.
	 * 
	 * @param object
	 *            Object to retrieve the feature value from.
	 * @param name
	 *            The feature name, or a method defined on {@link EObject} like 'eClass', 'eResource',
	 *            'eContainer', 'eContainingFeature', 'eContainmentFeature', 'eContents', 'eAllContents',
	 *            'eCrossReferences'
	 * @return Value of the given feature of the object
	 * @throws FactoryException
	 *             Thrown if the retrieval fails.
	 */
	public static Object eGet(EObject object, String name) throws FactoryException {
		Object result = null;
		try {
			final EStructuralFeature feature = eStructuralFeature(object, name);
			result = object.eGet(feature);
		} catch (FactoryException eGet) {
			try {
				result = eCall(object, name, null);
			} catch (FactoryException eCall) {
				throw eGet;
			}
		} catch (NullPointerException eCall) {
			// Fails silently
		}
		if (result != null && result instanceof Enumerator) {
			result = ((Enumerator)result).getName();
		} else if (result != null && result instanceof EDataTypeUniqueEList) {
			final List<Object> list = new ArrayList<Object>();
			final Iterator enums = ((EDataTypeUniqueEList)result).iterator();
			while (enums.hasNext()) {
				final Object next = enums.next();
				if (next instanceof Enumerator) {
					list.add(((Enumerator)next).getName());
				} else {
					list.add(next);
				}
			}
			result = list;
		}
		return result;
	}

	/**
	 * Gets the structural feature of the given feature name of the object.
	 * 
	 * @param object
	 *            Object to retrieve the feature from.
	 * @param name
	 *            Name of the feature to retrieve.
	 * @return The structural feature <code>name</code> of <code>object</code>.
	 * @throws FactoryException
	 *             Thrown if the retrieval fails.
	 */
	public static EStructuralFeature eStructuralFeature(EObject object, String name) throws FactoryException {
		final EStructuralFeature structuralFeature = object.eClass().getEStructuralFeature(name);
		if (structuralFeature != null) {
			return structuralFeature;
		} else {
			throw new FactoryException("The link '" + name + "' doesn't exist in the class '" //$NON-NLS-1$ //$NON-NLS-2$
					+ object.eClass().getName() + "'"); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the value of the given feature of the object, as an EObject.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return the value or null if it isn't an EObject
	 * @throws FactoryException
	 */
	public static EObject eGetAsEObject(EObject object, String name) throws FactoryException {
		Object eGet = eGet(object, name);
		if (eGet != null && eGet instanceof EObject)
			return (EObject)eGet;
		else
			return null;
	}

	/**
	 * Gets the value of the given feature of the object, as a String.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return the value or null if it isn't a String
	 * @throws FactoryException
	 */
	public static String eGetAsString(EObject object, String name) throws FactoryException {
		Object eGet = eGet(object, name);
		if (eGet != null)
			return eGet.toString();
		else
			return null;
	}

	/**
	 * Gets the value of the given feature of the object, as a Boolean.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return the value or null if it isn't a Boolean
	 * @throws FactoryException
	 */
	public static Boolean eGetAsBoolean(EObject object, String name) throws FactoryException {
		Object eGet = eGet(object, name);
		if (eGet != null && eGet instanceof Boolean)
			return (Boolean)eGet;
		else
			return null;
	}

	/**
	 * Gets the value of the given feature of the object, as an Integer.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return the value or null if it isn't an Integer
	 * @throws FactoryException
	 */
	public static Integer eGetAsInteger(EObject object, String name) throws FactoryException {
		Object eGet = eGet(object, name);
		if (eGet != null && eGet instanceof Integer)
			return (Integer)eGet;
		else
			return null;
	}

	/**
	 * Gets the value of the given feature of the object, as a List.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return the value, or a new List with a single element if it isn't a List, or null if it doesn't exist
	 * @throws FactoryException
	 */
	public static List eGetAsList(EObject object, String name) throws FactoryException {
		Object eGet = eGet(object, name);
		if (eGet != null) {
			if (eGet instanceof List) {
				return (List)eGet;
			} else {
				List list = new BasicEList(1);
				list.add(eGet);
				return list;
			}
		} else {
			return null;
		}
	}

	/**
	 * Indicates if the object is instance of the class whose name is given.
	 * <p>
	 * Samples :
	 * <p>
	 * An instance of java.resources.Folder return true if name equals "Folder" or "resources.Folder".
	 * <p>
	 * An instance of java.resources.Folder return true if name equals "File" and Folder inherits File.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the class name
	 * @return true if the object is instance of the class whose name is given
	 */
	public static boolean eInstanceOf(EObject object, String name) {
		if (object == null)
			return (name == null);
		return eInstanceOf(object.eClass(), name);
	}

	private static boolean eInstanceOf(EClass eClass, String name) {
		if (name.indexOf(".") == -1 && name.equals(eClass.getName())) {
			return true;
		} else {
			String instanceClassName = "." + eClass.getInstanceClassName();
			String endsWith = "." + name;
			if (instanceClassName.endsWith(endsWith)) {
				return true;
			} else {
				Iterator superTypes = eClass.getESuperTypes().iterator();
				while (superTypes.hasNext()) {
					EClass eSuperClass = (EClass)superTypes.next();
					if (eInstanceOf(eSuperClass, name))
						return true;
				}
				return false;
			}
		}
	}

	/**
	 * Indicates if the feature name given is valid for the object.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return true if the feature is defined, false if not
	 */
	public static boolean eValid(EObject object, String name) {
		try {
			eGet(object, name);
			return true;
		} catch (FactoryException e) {
			return false;
		}
	}

	/**
	 * Indicates if the object has a value for the feature name.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @return if the feature is a list, return feature.size() > 0 else return feature != null
	 */
	public static boolean eExist(EObject object, String name) {
		try {
			Object eGet = eGet(object, name);
			if (eGet != null) {
				if (eGet instanceof List) {
					return ((List)eGet).size() > 0;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} catch (FactoryException e) {
			return false;
		}
	}

	/**
	 * Indicates if the object contains the given value for the feature name.
	 * 
	 * @param object
	 *            is the object
	 * @param name
	 *            is the feature name
	 * @param arg
	 *            is the value to find, null is allowed
	 * @return true if the object contains the given value for the feature name
	 */
	public static boolean eExist(EObject object, String name, Object arg) {
		try {
			Object eGet = eGet(object, name);
			if (eGet != null && eGet instanceof List) {
				return ((List)eGet).contains(arg);
			} else {
				return (eGet == arg);
			}
		} catch (FactoryException e) {
			return false;
		}
	}

}
