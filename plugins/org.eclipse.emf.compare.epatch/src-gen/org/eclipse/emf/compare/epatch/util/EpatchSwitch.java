/**
 * <copyright>
 * </copyright>
 *
 */
package org.eclipse.emf.compare.epatch.util;

import java.util.List;

import org.eclipse.emf.compare.epatch.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> The <b>Switch</b> for the model's inheritance hierarchy. It supports the call
 * {@link #doSwitch(EObject) doSwitch(object)} to invoke the <code>caseXXX</code> method for each class of the
 * model, starting with the actual class of the object and proceeding up the inheritance hierarchy until a
 * non-null result is returned, which is the result of the switch. <!-- end-user-doc -->
 * 
 * @see org.eclipse.emf.compare.epatch.EpatchPackage
 * @generated
 */
public class EpatchSwitch<T> {
	/**
	 * The cached model package <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected static EpatchPackage modelPackage;

	/**
	 * Creates an instance of the switch. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public EpatchSwitch() {
		if (modelPackage == null) {
			modelPackage = EpatchPackage.eINSTANCE;
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields
	 * that result. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	public T doSwitch(EObject theEObject) {
		return doSwitch(theEObject.eClass(), theEObject);
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields
	 * that result. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected T doSwitch(EClass theEClass, EObject theEObject) {
		if (theEClass.eContainer() == modelPackage) {
			return doSwitch(theEClass.getClassifierID(), theEObject);
		} else {
			List<EClass> eSuperTypes = theEClass.getESuperTypes();
			return eSuperTypes.isEmpty() ? defaultCase(theEObject) : doSwitch(eSuperTypes.get(0), theEObject);
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields
	 * that result. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case EpatchPackage.EPATCH: {
				Epatch epatch = (Epatch)theEObject;
				T result = caseEpatch(epatch);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.IMPORT: {
				Import import_ = (Import)theEObject;
				T result = caseImport(import_);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.MODEL_IMPORT: {
				ModelImport modelImport = (ModelImport)theEObject;
				T result = caseModelImport(modelImport);
				if (result == null)
					result = caseImport(modelImport);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.RESOURCE_IMPORT: {
				ResourceImport resourceImport = (ResourceImport)theEObject;
				T result = caseResourceImport(resourceImport);
				if (result == null)
					result = caseModelImport(resourceImport);
				if (result == null)
					result = caseImport(resourceImport);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.EPACKAGE_IMPORT: {
				EPackageImport ePackageImport = (EPackageImport)theEObject;
				T result = caseEPackageImport(ePackageImport);
				if (result == null)
					result = caseModelImport(ePackageImport);
				if (result == null)
					result = caseImport(ePackageImport);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.JAVA_IMPORT: {
				JavaImport javaImport = (JavaImport)theEObject;
				T result = caseJavaImport(javaImport);
				if (result == null)
					result = caseImport(javaImport);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.EXTENSION_IMPORT: {
				ExtensionImport extensionImport = (ExtensionImport)theEObject;
				T result = caseExtensionImport(extensionImport);
				if (result == null)
					result = caseImport(extensionImport);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.NAMED_RESOURCE: {
				NamedResource namedResource = (NamedResource)theEObject;
				T result = caseNamedResource(namedResource);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.NAMED_OBJECT: {
				NamedObject namedObject = (NamedObject)theEObject;
				T result = caseNamedObject(namedObject);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.OBJECT_REF: {
				ObjectRef objectRef = (ObjectRef)theEObject;
				T result = caseObjectRef(objectRef);
				if (result == null)
					result = caseNamedObject(objectRef);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.CREATED_OBJECT: {
				CreatedObject createdObject = (CreatedObject)theEObject;
				T result = caseCreatedObject(createdObject);
				if (result == null)
					result = caseNamedObject(createdObject);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.ASSIGNMENT: {
				Assignment assignment = (Assignment)theEObject;
				T result = caseAssignment(assignment);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.SINGLE_ASSIGNMENT: {
				SingleAssignment singleAssignment = (SingleAssignment)theEObject;
				T result = caseSingleAssignment(singleAssignment);
				if (result == null)
					result = caseAssignment(singleAssignment);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.LIST_ASSIGNMENT: {
				ListAssignment listAssignment = (ListAssignment)theEObject;
				T result = caseListAssignment(listAssignment);
				if (result == null)
					result = caseAssignment(listAssignment);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.ASSIGNMENT_VALUE: {
				AssignmentValue assignmentValue = (AssignmentValue)theEObject;
				T result = caseAssignmentValue(assignmentValue);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.OBJECT_NEW: {
				ObjectNew objectNew = (ObjectNew)theEObject;
				T result = caseObjectNew(objectNew);
				if (result == null)
					result = caseCreatedObject(objectNew);
				if (result == null)
					result = caseNamedObject(objectNew);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			case EpatchPackage.OBJECT_COPY: {
				ObjectCopy objectCopy = (ObjectCopy)theEObject;
				T result = caseObjectCopy(objectCopy);
				if (result == null)
					result = caseCreatedObject(objectCopy);
				if (result == null)
					result = caseNamedObject(objectCopy);
				if (result == null)
					result = defaultCase(theEObject);
				return result;
			}
			default:
				return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Epatch</em>'. <!-- begin-user-doc
	 * --> This implementation returns null; returning a non-null result will terminate the switch. <!--
	 * end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Epatch</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseEpatch(Epatch object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Import</em>'. <!-- begin-user-doc
	 * --> This implementation returns null; returning a non-null result will terminate the switch. <!--
	 * end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseImport(Import object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Model Import</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Model Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseModelImport(ModelImport object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Resource Import</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Resource Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseResourceImport(ResourceImport object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EPackage Import</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EPackage Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseEPackageImport(EPackageImport object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Java Import</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Java Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseJavaImport(JavaImport object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Extension Import</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Extension Import</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseExtensionImport(ExtensionImport object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Named Resource</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Named Resource</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseNamedResource(NamedResource object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Named Object</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Named Object</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseNamedObject(NamedObject object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Object Ref</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Object Ref</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseObjectRef(ObjectRef object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Created Object</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Created Object</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseCreatedObject(CreatedObject object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Assignment</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Assignment</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAssignment(Assignment object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Single Assignment</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Single Assignment</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseSingleAssignment(SingleAssignment object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>List Assignment</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>List Assignment</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseListAssignment(ListAssignment object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Assignment Value</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Assignment Value</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAssignmentValue(AssignmentValue object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Object New</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Object New</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseObjectNew(ObjectNew object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Object Copy</em>'. <!--
	 * begin-user-doc --> This implementation returns null; returning a non-null result will terminate the
	 * switch. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Object Copy</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseObjectCopy(ObjectCopy object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'. <!-- begin-user-doc
	 * --> This implementation returns null; returning a non-null result will terminate the switch, but this
	 * is the last case anyway. <!-- end-user-doc -->
	 * 
	 * @param object
	 *            the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	public T defaultCase(EObject object) {
		return null;
	}

} // EpatchSwitch
