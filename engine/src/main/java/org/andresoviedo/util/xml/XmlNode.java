package org.andresoviedo.util.xml;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public final class XmlNode {
	/**********************************************************************************************/
	private String name;
	/**********************************************************************************************/
	private Map<String, String> attributes;
	/**********************************************************************************************/
	private String data;
	/**********************************************************************************************/
	private Map<String, List<XmlNode>> childNodes;

	/**********************************************************************************************/
	XmlNode(String name) {
		this.name = name;
	}

	/**********************************************************************************************/
	public String getName() {
		return name;
	}

	/**********************************************************************************************/
	public String getData() {
		return data;
	}

	/**********************************************************************************************/
	public String getAttribute(String attr) {
		if (attributes != null) {
			return attributes.get(attr);
		} else {
			return null;
		}
	}

	/**********************************************************************************************/
	public XmlNode getChild(String childName) {
		if (childNodes != null) {
			List<XmlNode> nodes = childNodes.get(childName);
			if (nodes != null && !nodes.isEmpty()) {
				return nodes.get(0);
			}
		}
		return null;
	}

	/**********************************************************************************************/
	public XmlNode getChildWithAttribute(String childName, String attr, String value) {
		List<XmlNode> children = getChildren(childName);

		if (children == null || children.isEmpty()) {
			return null;
		}

		for (XmlNode child : children) {
			String val = child.getAttribute(attr);
			if (value.equals(val)) {
				return child;
			}
		}

		return null;
	}

	/**********************************************************************************************/
	public XmlNode getChildWithAttributeRecursive(String childName, String attr, String value) {
		List<XmlNode> children = getChildren(childName);

		if (children == null || children.isEmpty()) {
			return null;
		}

		for (XmlNode child : children) {
			String val = child.getAttribute(attr);

			if (value.equals(val)) {
				return child;
			} else {
				XmlNode candidate = null;
				candidate = child.getChildWithAttributeRecursive(childName, attr, value);
				if (candidate != null) return candidate;
			}
		}
		return null;
	}

	/**********************************************************************************************/
	@NonNull
	public List<XmlNode> getChildren(String name) {
		if (childNodes != null) {
			List<XmlNode> children = childNodes.get(name);

			if (children != null) {
				return children;
			}
		}
		return Collections.emptyList();
	}

	/**********************************************************************************************/
	protected void addAttribute(String attr, String value) {
		if (attributes == null) {
			attributes = new HashMap<>();
		}
		attributes.put(attr, value);
	}

	/**********************************************************************************************/
	protected void addChild(XmlNode child) {
		if (childNodes == null) {
			childNodes = new HashMap<>();
		}

		List<XmlNode> list = childNodes.get(child.name);

		if (list == null) {
			list = new ArrayList<>();
			childNodes.put(child.name, list);
		}

		list.add(child);
	}

	/**********************************************************************************************/
	protected void setData(String data) {
		this.data = data;
	}
}
