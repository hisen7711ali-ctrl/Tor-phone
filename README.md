[![Build](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/workflows/android-build.yml/badge.svg?branch=main)](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/workflows/android-build.yml)
![Latest Release](https://img.shields.io/github/v/release/hisen7711ali-ctrl/Tor-phone)
![Size](https://img.shields.io/badge/APK%20size-~68%20MB-orange)

# Tor-phone

تطبيق أندرويد تجريبي يدمج خدمة VPN في Android مع محرك Tor مبني على Arti وOnionmasq. الهدف هو توجيه حركة الشبكة عبر Tor من خلال نفق VPN محلي، دون الاعتماد على تطبيق Orbot منفصل.

## ما هو التطبيق؟

يستخدم التطبيق واجهة `VpnService` لإنشاء واجهة شبكة افتراضية، ثم يسلّم واجهة TUN إلى محرك Onionmasq لمعالجة الحزم وتوجيه الاتصالات المدعومة عبر Tor. يضبط الكود مسارات IPv4 وIPv6 افتراضية داخل الواجهة؛ لكن وجود المسارات أو ظهور رمز VPN لا يثبت وحده أن حركة التطبيقات خرجت من عقدة Tor.

## الحالة الحالية

حالة المشروع: **تجريبي، مبني آليًا، ولم يُتحقق منه على هاتف حقيقي**.

نجح بناء المكتبات الأصلية للمعماريتين، ونجح بناء نسخ APK المنفصلة وملف AAB في GitHub Actions. أُزيلت نسخة مكتبة أصلية مكررة كانت تُضمّن مرتين داخل كل APK. أحجام النسخ المقاسة في سير البناء `41702169900`:

| الملف | الحجم المقاس |
|---|---:|
| `app-arm64-v8a-release-unsigned.apk` | 59.05 ميغابايت |
| `app-armeabi-v7a-release-unsigned.apk` | 50.74 ميغابايت |
| `app-release.aab` | 52.05 ميغابايت |

ملفات Release وAAB غير موقعة بمفتاح إصدار؛ لذلك لا تُعامل على أنها حزم جاهزة للتثبيت أو النشر. نسخة Debug موقعة بمفتاح التطوير ويمكن استخدامها للاختبار الأولي.

## ما الذي يعمل فعليًا في البناء الحالي؟

- بناء مكتبات Onionmasq الأصلية لمعمارِيتي `arm64-v8a` و`armeabi-v7a` في GitHub Actions.
- وجود المكتبات الناتجة في `app/src/main/jniLibs/`، مع تضمين مكتبة JNI التي يستدعيها التطبيق دون نسخة اسمية مكررة.
- نجاح تجميع APK منفصل لكل معمارية وملف AAB.
- وجود خدمة `VpnService` وتدفق طلب موافقة المستخدم على VPN.
- وجود تكامل برمجي مع واجهة Android الخاصة بـ Onionmasq، ونجاح بناء المكتبات والتطبيق.
- استخدام مسارات افتراضية IPv4 وIPv6 في إعداد واجهة VPN.
- استخراج النصوص الظاهرة إلى موارد عربية وإنجليزية، مع مبدّل لغة محفوظ.
- تفعيل `android:supportsRtl` وملف اللغات `locales_config.xml`، واستخدام إعداد اللغة الخاص بالتطبيق في Android 13 وما بعده، و`SharedPreferences` في الإصدارات الأقدم.
- وجود أيقونة متجهية وتكيفية ودائرية وطبقة أحادية اللون، وصور قديمة مولّدة من المصدر المتجهي لخمس كثافات، وشاشة بدء.
- وجود دليل اختبار الجهاز الحقيقي في [TEST.md](TEST.md).
- حالة الواجهة تعرض حالة الخدمة المرسلة من `TorPhoneVpnService`، وتستخدم «غير معروفة» عند عدم توفر حالة مؤكدة. حالة تشغيل المحرك لا تُعرض على أنها إثبات لاتصال Tor أو تغيّر عنوان IP.

## ما الذي لم يُنجز أو لم يُثبت؟

- لم يُثبّت التطبيق بعد على هاتف حقيقي ضمن اختبار موثق.
- أضيف فحص HTTPS إلى `check.torproject.org/api/ip`؛ لا تظهر الحالة الخضراء إلا عند استلام `IsTor: true`. يحتاج ذلك إلى اختبار فعلي على الجهاز والشبكة المستخدمة.
- لم تُجرَ اختبارات تسرب DNS أو IPv6 على جهاز فعلي.
- لم يُختبر Kill Switch عمليًا، ولم يُثبت منع الاتصال المباشر عند تعطل الخدمة.
- يعلن التطبيق دعم `Always-on VPN` في بيان Android؛ يجب على المستخدم تفعيل `Block connections without VPN` يدويًا لاختبار Lockdown.
- أضيفت شاشة `Split Tunneling` لحصر التطبيقات المستثناة من نفق Tor عبر `addDisallowedApplication`. التغييرات تُطبّق عند إعادة اتصال VPN، والتطبيقات المستثناة لا تستفيد من Tor. لم يُختبر ذلك بعد على جهاز حقيقي.
- مكتبات Onionmasq الأصلية المثبتة في هذا المشروع تُبنى مع ميزة `pluggable-transports`، وتتضمن تكامل `IPtProxy` ومحلل Bridge lines لأنواع `obfs4` و`Snowflake`. لكن طبقة التطبيق الحالية تستدعي `OnionMasq.start(fd)` من دون تمرير `bridgeLines`؛ لذلك دعم النقل موجود في native build لكنه غير قابل للتفعيل من واجهة التطبيق بعد. ربط الإعدادات يتطلب تعديل مسار تشغيل الـ adapter لاستدعاء overload الذي يستقبل Bridge lines، وهذا لم يُنفّذ احترامًا لقيد عدم تعديل `OnionmasqRouter` أو منطق الخدمة خارج نطاق IP verification وSplit Tunneling.
- لم تُنفذ واجهة Proton VPN النهائية أو الحركات الدقيقة المرتبطة بحالات اتصال مؤكدة.
- نظام الألوان والقياسات والخطوط مركزي في الموارد، لكن الوضع الفاتح الكامل والحركات الدقيقة لم يُستكملا بعد.
- لم تُجرَ اختبارات ترجمة وRTL على أجهزة فعلية؛ وجود الموارد والمبدّل لا يغني عن الاختبار.
- لم يُختبر سلوك الأيقونة وشاشة البدء على إصدارات Android المختلفة فعليًا.

## تحذيرات أمنية

### GPS والموقع

VPN نفق شبكي، ولا يعطّل مستقبل GPS ولا يمنع استقبال إشارات الأقمار الصناعية. لا يجوز عرض «GPS محمي» أو «GPS موقوف» في الوضع العادي. تعطيل الموقع على مستوى الجهاز عبر `DevicePolicyManager.setLocationEnabled(false)` يتطلب وضع مالك الجهاز أو مالك ملف العمل، مع إجراءات إعداد وإدارة جهاز قد تتطلب إعادة ضبط المصنع. صلاحية Device Admin العادية لا تمنح هذه القدرة. حجب GPS غير منفذ في هذا التطبيق.

### توجيه جميع التطبيقات

لا يوجد في هذا الإصدار إثبات أن كل تطبيق يمر عبر النفق. لا تَعِد بحماية مطلقة لكل التطبيقات أو كل أنواع DNS. إدارة الجهاز قد تسمح بتفعيل VPN دائم التشغيل وقفل الشبكة في الأجهزة المُدارة، لكنها ليست بديلًا عن اختبارات التسرب ولا تبرر ادعاء ضمان مطلق. بعض التطبيقات قد تتجاوز إعدادات VPN أو تستخدم DNS مشفرًا خاصًا.

## بناء المشروع

1. استنسخ المستودع مع الوحدة الفرعية:
   `git clone --recurse-submodules https://github.com/hisen7711ali-ctrl/Tor-phone.git`
2. افتح المشروع باستخدام Android Studio، أو جهّز Java 17 وAndroid SDK 35 وGradle 8.9.
3. بناء المكتبات الأصلية يتطلب Android NDK r27d، وRust 1.91.0، و`cargo-ndk` بالإصدار 3.5.4.
4. نفّذ سير العمل `Build Onionmasq native libraries` لبناء مكتبات Android.
5. تُحفظ المكتبات الناتجة في `app/src/main/jniLibs/<abi>/`.
6. نفّذ `gradle assembleDebug` لبناء نسخة Debug.
7. نفّذ `gradle assembleRelease bundleRelease` لبناء APK وAAB. يتطلب التوزيع توقيعًا بمفتاح إصدار يملكه صاحب التطبيق.
8. نجاح البناء لا يغني عن تثبيت التطبيق وإجراء اختبارات IP وDNS وIPv6 وKill Switch على هاتف حقيقي.

## الترخيص

لا يوجد في جذر هذا المستودع ملف ترخيص مستقل يحدد ترخيص التطبيق نفسه. يحتفظ مكوّن Onionmasq upstream بملفي الترخيص Apache-2.0 وMIT؛ راجع ملفات الترخيص في مصدر المكوّن قبل إعادة التوزيع. لا يعني ترخيص المكوّن تلقائيًا أن التطبيق كاملًا يحمل الترخيص نفسه.

## الالتزامات الرئيسية

- [إضافة خدمة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/f73df844255d83026d82ea827721d6e050e052c0)
- [تدفق موافقة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/6547de59fbb48100bed3abed1ccb8670b14f56d3)
- [ربط Onionmasq بخدمة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/3c2fe3ff617de10987c57346f721970abc8e4e08)
- [دليل الاختبار على الهاتف](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/928d80295efb6a18542e5d82153a126a70f67540)
- [الأيقونة المتجهية والتكيفية وشاشة البدء](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/3bc2695229007f22f753a201762bdcf1412bbbc5)
- [توليد صور الأيقونة للكثافات المختلفة](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/f6deb328c5d1eb09568322e30e7138c64359306a)
- [إزالة المكتبة المكررة وإصلاح شاشة البدء](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/7c681d41f273cbaafe863bdc090a85c23a19e28c)
- [موارد العربية والإنجليزية ومبدّل اللغة](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/d3f76fdba25cecab6fe0cd99c048253afc4beb7e)
- [ربط حالة الواجهة بحالة الخدمة وتحسين حفظ اللغة](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/f64633e54a05ce48e7377ac8f85862728ac5a2e0)
- [ضبط مساحة الأمان للأيقونة](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/fc8439519a5cf2ef1561a1902d4408e3f5cd59c1)

## روابط الاختبار والبناء

- [دليل الاختبار](TEST.md)
- [سير بناء APK وAAB وقياس الأحجام](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/runs/41702169900)
- [سير توليد صور الأيقونة](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/runs/41702169977)
- [سير بناء مكتبات Onionmasq](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/runs/41699948909)


## Screenshots

لقطات الشاشة غير مضافة بعد. أضف الصور لاحقًا داخل مجلد `screenshots/`، ثم حدّث المسارات التالية:

| الشاشة | المسار المقترح |
|---|---|
| الشاشة الرئيسية | `screenshots/home.png` |
| حالة الاتصال | `screenshots/connection-status.png` |
| الإعدادات | `screenshots/settings.png` |
| تقسيم النفق | `screenshots/split-tunneling.png` |

عند إضافة الصور، أزل النصوص الحساسة وأي عناوين IP أو بيانات حسابات من اللقطات.

## Roadmap

خارطة الطريق التفصيلية ومراحل الاعتماد ومعايير الإنجاز والتقديرات موجودة في [PROJECT_PLAN.md](PROJECT_PLAN.md). المراحل المخطط لها ليست ميزات منفذة، ولا يبدأ العمل اللاحق قبل استكمال بوابة الاختبار المحددة في الخطة.

## Privacy

راجع [سياسة الخصوصية بالعربية والإنجليزية](PRIVACY_POLICY.md) و[سجل التغييرات](CHANGELOG.md). المشروع تجريبي ولا يدّعي أن ميزاته اجتازت اختبار هاتف شامل.
