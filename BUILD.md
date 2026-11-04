# بناء Onionmasq

## المصدر المثبّت

- إصدار التطبيق المرجعي: `Tor VPN 1.9.1Beta`، رقما البناء 241 و242.
- مصدر Onionmasq: الوحدة الفرعية `third_party/onionmasq/vendor`.
- الالتزام المثبّت: `fc27c71d341ee2b8a6cb5db534458ad598a14135`.
- مرجع التثبيت: بيانات F-Droid الرسمية في `metadata/org.torproject.vpn.yml`.
- مرآة GitHub المستخدمة للتحقق من الالتزام: `https://github.com/syphyr/onionmasq`.

## البناء الآلي

يستخدم سير العمل `.github/workflows/build-onionmasq.yml` نظام Android NDK r27d
(`27.3.13750724`)، وRust، و`cargo-ndk` لبناء:

- `arm64-v8a/libonionmasq_mobile.so`
- `armeabi-v7a/libonionmasq_mobile.so`

ثم يضع الملفات داخل `app/src/main/jniLibs/<abi>/` في مساحة عمل البناء،
ويجمع التطبيق ويرفع المكتبات وملف APK كملفات ناتجة من سير العمل.

## واجهة Android الرسمية

لا توجد واجهة JNI مستقلة اخترعناها لهذا التطبيق. يستخدم الجسر واجهة Onionmasq
الرسمية الموجودة في الوحدة الفرعية:

- `org.torproject.onionmasq.OnionMasq.init(Context)`
- `org.torproject.onionmasq.OnionMasq.start(int fd)`
- `org.torproject.onionmasq.OnionMasq.isRunning()`
- `org.torproject.onionmasq.OnionMasq.stop()`

تتولى `OnionMasqJni` تحميل `libonionmasq_mobile.so` واستدعاء واجهات Rust.
الدالة `start(int fd)` تستحوذ على واصف TUN وتدير تمرير الحزم داخلياً؛ لذلك
لا يجوز قراءة الواصف نفسه بالتوازي من حلقة Java أخرى.

## ملاحظات السلامة

- لا تعني موافقة Android على إذن VPN أن Tor بدأ أو أن الحماية أصبحت فعالة.
- لا يعلن التطبيق الحماية إلا بعد أن تؤكد المكتبة أن الوكيل يعمل.
- أي فشل في تحميل المكتبة أو تهيئة Arti أو تشغيل Onionmasq يؤدي إلى إغلاق النفق.
- لا يُمرر أي اتصال إلى الشبكة العادية كمسار احتياطي.


## Release Signing

### إنشاء مفتاح الإصدار محليًا

أنشئ ملف keystore على جهاز موثوق تملكه، ولا تنشئه داخل GitHub Actions:

```bash
keytool -genkeypair -v -keystore tor-phone-release.jks -alias torphone -keyalg RSA -keysize 4096 -validity 10000
```

احتفظ بملف `tor-phone-release.jks` وكلمات المرور ونسخة احتياطية مشفرة في مكان آمن. لا ترفع الملف إلى المستودع، ولا ترسله في Issue أو محادثة عامة.

### أسرار GitHub المطلوبة

من إعدادات المستودع، افتح **Settings → Secrets and variables → Actions** وأنشئ الأسرار التالية:

- `KEYSTORE_BASE64`: محتوى ملف keystore بعد تحويله إلى Base64.
- `KEYSTORE_PASSWORD`: كلمة مرور ملف keystore.
- `KEY_ALIAS`: اسم المفتاح، مثل `torphone`.
- `KEY_PASSWORD`: كلمة مرور المفتاح.

لتحويل الملف إلى Base64 على Linux:

```bash
base64 -w 0 tor-phone-release.jks
```

على PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("tor-phone-release.jks"))
```

انسخ الناتج إلى السر `KEYSTORE_BASE64`. لا تضع الناتج في ملف متعقب في Git.

### مثال إعداد Gradle — للتوثيق فقط

المثال التالي يوضح طريقة قراءة القيم من متغيرات البيئة. **لم يُعدّل `app/build.gradle` ضمن هذه المهمة**. يجب أن يقوم سير العمل بفك Base64 إلى ملف مؤقت آمن، ثم يضبط متغيرات البيئة قبل تشغيل Gradle.

```groovy
def releaseStoreFile = System.getenv("KEYSTORE_PATH")

android {
    signingConfigs {
        release {
            if (releaseStoreFile != null) {
                storeFile file(releaseStoreFile)
                storePassword System.getenv("KEYSTORE_PASSWORD")
                keyAlias System.getenv("KEY_ALIAS")
                keyPassword System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

في سير العمل، تُقرأ الأسرار من `${{ secrets.KEYSTORE_BASE64 }}` وما يقابلها، ويُفك ملف keystore إلى مسار مؤقت لا يُرفع كـ artifact. احذف الملف المؤقت بعد انتهاء البناء، ولا تطبع الأسرار أو قيمها في السجل. لا تجعل البناء يوقّع Release بمفتاح Debug كحل بديل.

### الفرق بين Debug وRelease

- **Debug signing:** توقيع تطوير تلقائي يوفره Android Gradle Plugin. مناسب للتثبيت والاختبار الداخلي، وليس هو مفتاح هوية إصدار التطبيق للنشر.
- **Release signing:** توقيع بمفتاح إصدار خاص يملكه ناشر التطبيق. يلزم لإصدار APK/AAB موثوق ومتسق التحديثات، وللنشر على Google Play أو F-Droid وفق متطلبات كل قناة.
- إذا فُقد مفتاح التوقيع أو تغيرت بياناته فقد تتعذر ترقية النسخ المنشورة بالطريقة المعتادة. احتفظ بنسخة احتياطية آمنة.

**تحذير قاطع:** لا تلتزم بملف keystore أو كلمات مروره أو Base64 الخاص به في Git، ولا تضعها في README أو Issues أو ملفات إعداد عادية. استخدم GitHub Secrets، وقلّل صلاحيات الوصول إليها.
