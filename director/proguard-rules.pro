# Retain constructor that is called by using reflection to recreate the Controller
-keepclassmembers public class * extends com.ivianuu.director.Controller {
   public <init>();
}

-keepclassmembers public class * extends com.ivianuu.director.ControllerChangeHandler {
   public <init>();
}

-keepclassmembers public class * extends com.ivianuu.director.RouterTransaction {
   public <init>();
}