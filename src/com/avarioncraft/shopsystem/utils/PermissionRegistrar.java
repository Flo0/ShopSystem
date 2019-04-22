package com.avarioncraft.shopsystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PermissionRegistrar {

	public static Permission addPermission(String perm) {
		return addPermission(perm, "<Keine Beschreibung angegeben>", PermissionDefault.OP);
	}

	public static Permission addPermission(String perm, String description) {
		return addPermission(perm, description, PermissionDefault.OP);
	}

	public static Permission addPermission(String perm, String description, PermissionDefault defaultValue) {
		try {
			Permission permission = new Permission(perm, description, defaultValue);
			Bukkit.getPluginManager().addPermission(permission);
			return permission;
		} catch (IllegalArgumentException ex) {
			return Bukkit.getPluginManager().getPermission(perm);
		}
	}
}