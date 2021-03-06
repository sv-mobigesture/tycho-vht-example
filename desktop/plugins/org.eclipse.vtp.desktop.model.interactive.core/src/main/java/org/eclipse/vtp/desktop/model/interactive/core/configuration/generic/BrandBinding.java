/*--------------------------------------------------------------------------
 * Copyright (c) 2009 OpenMethods, LLC
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Trip Gilman (OpenMethods)
 *    - initial API and implementation
 -------------------------------------------------------------------------*/
package org.eclipse.vtp.desktop.model.interactive.core.configuration.generic;

import java.io.PrintStream;

import org.eclipse.vtp.desktop.model.core.branding.IBrand;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class represents the binding of a binding item to a specific brand.
 * The inheritance model of brands is also implemented here.  If this binding
 * does not have an explicit value, the value contained by this binding's parent
 * will be returned. The data contained by this binding is stored and loaded by
 * this class.
 * 
 * @author trip
 */
public class BrandBinding
{
	/**	The binding manager that contains this brand binding */
	private GenericBindingManager manager = null;
	/**	The brand associated with this binding */
	private IBrand brand = null;
	/**	The binding item (value) bound to the brand */
	private BindingItem bindingItem = null;
	/**	This binding's parent binding */
	private BrandBinding parentBinding = null;

	/**
	 * Creates a new brand binding instance contained by the given manager and
	 * associated with the provided brand.  Initially the binding item is null.
	 * 
	 * @param manager The manager that will contain this brand binding
	 * @param brand The brand this binding is associated with
	 */
	public BrandBinding(GenericBindingManager manager, IBrand brand)
	{
		super();
		this.manager = manager;
		this.brand = brand;
	}
	
	public Object clone()
	{
		BrandBinding copy = new BrandBinding(manager, brand);
		copy.bindingItem = bindingItem;
		copy.parentBinding = parentBinding;
		return copy;
	}
	
	/**
	 * @return The brand this binding is associated with
	 */
	public IBrand getBrand()
	{
		return brand;
	}
	
	/**
	 * Sets the parent binding of this brand binding.  The parent binding is
	 * used to implement brand inheritance.  The parent binding item is used if
	 * this binding's item is empty.
	 * 
	 * @param parentBinding The parent binding of this brand binding
	 */
	public void setParent(BrandBinding parentBinding)
	{
		this.parentBinding = parentBinding;
	}
	
	/**
	 * @return true if this brand binding has a parent, false otherwise
	 */
	public boolean hasParent()
	{
		return parentBinding != null;
	}
	
	/**
	 * Returns a boolean indicating if the value returned from
	 * {@link BrandBinding#getBindingItem()} was local to this brand binding or
	 * was retrieved from this binding's parent.
	 * 
	 * @return true if the value was retrieved from the parent binding.  false
	 * otherwise
	 */
	public boolean isInherited()
	{
		return bindingItem == null;
	}

	/**
	 * Returns the binding item associated with this brand binding, or the item
	 * associated with this binding's parent if this binding's item is null.
	 * Returns null if this binding's item is null and all of this binding's
	 * parents' items are null. 
	 * 
	 * @return The item associated with this brand binding or one of its
	 * parents.
	 */
	public BindingItem getBindingItem()
	{
		if(bindingItem == null)
		{
			if(parentBinding != null)
				return parentBinding.getBindingItem();
		}
		return bindingItem;
	}
	
	/**
	 * Associates this given binding item with this brand binding.  Any previous
	 * association is forgotten.
	 * 
	 * @param bindingItem The item associated with this brand binding
	 */
	public void setBindingItem(BindingItem bindingItem)
	{
		this.bindingItem = bindingItem;
	}
	
	/**
	 * Reads the configuration data stored in the given dom element into this
	 * brand binding instance.  Any previous information stored in this brand
	 * binding is lost.
	 * 
	 * @param brandBindingElement The dom element containing the configuration
	 */
	public void readConfiguration(Element brandBindingElement)
	{
		NodeList bindingItemElementList = brandBindingElement.getElementsByTagName("binding-item");
		if(bindingItemElementList.getLength() > 0)
		{
			Element bindingItemElement = (Element)bindingItemElementList.item(0);
			String bindingItemType = bindingItemElement.getAttribute("type");
			BindingItem bindingItem = BindingRegistry.getInstance().getBindingItem(bindingItemType);
			if(bindingItem != null)
			{
				bindingItem.readConfiguration(bindingItemElement);
				this.bindingItem = bindingItem;
			}
		}
	}
	
	/**
	 * Stores this brand binding's information into the given dom element.
	 * 
	 * @param brandBindingElement The dom element to hold this binding's data
	 */
	public void writeConfiguration(Element brandBindingElement)
	{
		if(bindingItem != null)
		{
			Element bindingItemElement = brandBindingElement.getOwnerDocument().createElement("binding-item");
			brandBindingElement.appendChild(bindingItemElement);
			bindingItemElement.setAttribute("type", bindingItem.getType());
			bindingItem.writeConfiguration(bindingItemElement);
		}
	}

	/**
	 * Prints this brand binding's information to the given print stream.  This
	 * is useful for logging and debugging.
	 * 
	 * @param out The print stream to write the information to
	 */
	public void dumpContents(PrintStream out)
	{
		out.println("[IBrand Binding] " + brand.getName() + "(" + brand.getId() + ")");
		out.println("Binding Item " + bindingItem);
	}
}
