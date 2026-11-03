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
