package us.wearecurio.security

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * An annotation which represents that the annotated method need not to be checked for login authentication.
 * This annotation is used to populate the list of methods in SecurityService that should not be checked for user authentication 
 * Example:
 * <pre>
 *      @NoAuth
 *      class SprintController {
 * 
 *      }
 *		
 *		class DiscussionController {
 *
 *			@NoAuth
 *			def show() { ... }
 * 
 *		}
 * </pre>
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAuth {

}
