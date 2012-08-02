/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class, controls the plug-in life cycle.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class EMFCompareIDEUIPlugin extends AbstractUIPlugin {
	/** The plugin ID. */
	public static final String PLUGIN_ID = "org.eclipse.emf.compare.ide.ui"; //$NON-NLS-1$

	/** Plug-in's shared instance. */
	private static EMFCompareIDEUIPlugin plugin;

	/** Manages the images that were loaded by EMF Compare. */
	private LocalResourceManager fResourceManager;

	/**
	 * Default constructor.
	 */
	public EMFCompareIDEUIPlugin() {
		// Empty implementation
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		if (fResourceManager != null) {
			fResourceManager.dispose();
		}
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance.
	 */
	public static EMFCompareIDEUIPlugin getDefault() {
		return plugin;
	}

	public ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(EMFCompareIDEUIPlugin.PLUGIN_ID, path);
	}

	public Image getImage(ImageDescriptor descriptor) {
		ResourceManager rm = getResourceManager();
		return rm.createImage(descriptor);
	}

	/**
	 * Loads an image from this plugin's path and returns it.
	 * 
	 * @param path
	 *            Path to the image we are to load.
	 * @return The loaded image.
	 */
	public Image getImage(String path) {
		final ImageDescriptor descriptor = imageDescriptorFromPlugin(EMFCompareIDEUIPlugin.PLUGIN_ID, path);
		Image result = null;
		if (descriptor != null) {
			ResourceManager rm = getResourceManager();
			result = rm.createImage(descriptor);
		}
		return result;
	}

	private synchronized ResourceManager getResourceManager() {
		if (fResourceManager == null) {
			fResourceManager = new LocalResourceManager(JFaceResources.getResources());
		}
		return fResourceManager;
	}
}
