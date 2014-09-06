
eventConfigureTomcat = {tomcat ->
    def ctx = tomcat.host.findChild("")
    ctx.allowLinking = true     // Used to follow soft links. Used for site development.
}