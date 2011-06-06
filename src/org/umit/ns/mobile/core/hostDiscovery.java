/*
Android Network Scanner Umit Project
Copyright (C) 2011 Adriano Monteiro Marques

Author: Angad Singh <angad@angad.sg>

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */

/**
 * @author angadsg
 * 
 * Implements various modes of Host Discovery
 * 1. Insane mode
 * 2. Normal mode
 * 3. Speed mode
 * 
 * In all modes, if ARP cache is available - uses that.
 * 
 * Insane - Uses all ping methods
 * Normal - isReachable, TCP Connect
 * Speed - isReachable
 * 
 * Implements Async Task for various modes.
 * 
 */

package org.umit.ns.mobile.core;

public class hostDiscovery {

}
