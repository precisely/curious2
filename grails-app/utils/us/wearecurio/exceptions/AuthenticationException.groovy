/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package us.wearecurio.exceptions

import org.apache.commons.logging.LogFactory

/**
 *
 * @author mitsu
 */
class AuthenticationException extends Exception {

	private static def log = LogFactory.getLog(this)

	public AuthenticationException(String text) {
		super(text);
	}
}
