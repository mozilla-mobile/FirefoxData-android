## todo
* Deps: I assume FxA versions must sync with Firefox to reduce our APK size.
  * Can we rm some deps? e.g. ActionBar is added in API 11 but we use support
  * Switch to official Apache httpclientandroidlib ?: https://hc.apache.org/httpcomponents-client-4.3.x/android-port.html
  * Apache commons from gradle instead of import files?
* l10n
* Delete Android account code
* Figure out RobocopTarget
* Where is NativeCrypto code? Do I need NDK?
* telemetry

### Support
* get tests working
* linters
  * android lint
  * checkstyle
* CI
* ProGuard
* Release build type

### Adminnstrivia
* attach example/test project
* License for non-MPL files.
* Mention how to build with local (so dev on mozilla-central)

### Later
* Remerge to fennec (grisha says leave out Android account stuff)
* drop ch.boye.httpclientandroidlib. nalexander says:

mcomella: it's big.  You don't want to ship it.  Using a different dep would be
easy.  You don't have to, and you can "trust" the HTTP/TLS configuration to
work, but you'll end up bring a _lot_ of Sync with you...
14:32 Well, maybe not that much.  But enough that it won't be a clean excision.
14:32 And I gave you the excision point; you should use it :)

mcomella: grisha: for sure!  mcomella, it shouldn't be too hard to do what you
want, pulling minimal deps from the FxALoginStateMachine.
14:27 mcomella: you should, of course, implement your own FxAClient20 or
whatever that doesn't use Apache's httpclientlib.
