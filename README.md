# Tor-phone

تطبيق أندرويد تجريبي يهدف إلى تمرير حركة مرور الشبكة عبر محرك Tor مدمج داخل خدمة VPN، دون الاعتماد على تطبيق Orbot منفصل.

## ما هو التطبيق؟

يجمع المشروع بين خدمة Android VPN ومحرك Onionmasq المبني على Arti. الهدف هو إنشاء نفق شبكي على مستوى الجهاز وتوجيه حركة التطبيقات عبر شبكة Tor. وجود مسارات افتراضية داخل Android لا يثبت وحده أن الحزم وصلت إلى Tor أو أن عنوان IP تغيّر.

## الحالة الحالية

المشروع في مرحلة التطوير والتحقق. نجح البناء الآلي لمكتبات Onionmasq الأصلية لمعمارِيتي `arm64-v8a` و`armeabi-v7a`، ونجح تجميع APK في GitHub Actions. لم يُقدَّم حتى الآن إثبات اختبار على هاتف حقيقي، ولا يوجد قياس موثق لتسرب DNS أو IPv6 أو نجاح تغيير عنوان IP عبر Tor. لذلك لا يُعد هذا الإصدار خدمة VPN/Tor جاهزة للاستخدام الحساس.

## ما الذي يعمل فعليًا الآن؟

- بناء مكتبات Onionmasq الأصلية للمعماريتين المذكورتين في سير GitHub Actions.
- حفظ ملفات المكتبات الثنائية الناتجة داخل `app/src/main/jniLibs/` في المستودع.
- تجميع نسخة APK تجريبية بنجاح في GitHub Actions.
- يحتوي التطبيق على خدمة `VpnService` وتكامل برمجي مع واجهة Android الخاصة بـ Onionmasq.
- يضبط كود الخدمة مسارات IPv4 وIPv6 افتراضية داخل واجهة VPN؛ هذا إعداد برمجي وليس إثباتًا لمرور الحزم عبر Tor.

## ما الذي لم يُنجز أو لم يُثبت؟

- تثبيت التطبيق وتشغيله والتحقق منه على هاتف Android حقيقي.
- إثبات وصول أول اتصال عبر Tor باستخدام فحص عنوان IP خارجي.
- اختبارات تسرب DNS وIPv6 عند الاتصال والانقطاع وتبديل الشبكات.
- اختبار Kill Switch عمليًا وإثبات منع الخروج خارج النفق.
- التحقق من Always-on VPN ومنع التسرب في جميع حالات النظام.
- إثبات توجيه حركة كل التطبيقات؛ لا يستطيع تطبيق عادي ضمان إجبار كل تطبيق على استخدام النفق، وقد تتجاوز بعض التطبيقات إعدادات VPN أو تستخدم DNS مشفرًا خاصًا. لا يوجد ضمان مطلق دون صلاحيات إدارة جهاز مناسبة أو صلاحيات نظام/جذر.
- إدارة خوادم أو أوضاع VPN مستقلة، وواجهة Proton VPN، واللغتان العربية والإنجليزية ومبدّل اللغة.
- تقليص حجم النسخ النهائية والتحقق من أحجام ملفات كل معمارية.
- حجب GPS على مستوى الجهاز.

## تحذير مهم بشأن GPS والموقع

خدمة VPN تعالج حركة الشبكة ولا تتحكم بمستقبل GPS. استقبال إشارات الأقمار الصناعية لا يمر عبر نفق VPN، ولذلك لا يجوز وصف GPS بأنه محمي أو متوقف بسبب تشغيل VPN. تعطيل الموقع على مستوى الجهاز يتطلب وضع مالك الجهاز أو ملف العمل وفق قيود Android، مع إعداد وإدارة جهاز مخصصة؛ أما صلاحية Device Admin العادية فلا تمنح هذه القدرة. في الوضع العادي يستطيع التطبيق فقط كشف حالة الموقع وإظهار تحذير وفتح إعدادات الموقع ليغيّر المستخدم الإعداد بنفسه.

## بناء المشروع خطوة بخطوة

1. استنسخ المستودع مع الوحدة الفرعية:
   `git clone --recurse-submodules https://github.com/hisen7711ali-ctrl/Tor-phone.git`
2. افتح المشروع باستخدام Android Studio أو ثبّت Java 17 وAndroid SDK 35 وGradle المتوافق.
3. لبناء مكتبات Onionmasq محليًا، جهّز Android NDK r27d وRust 1.91.0 و`cargo-ndk` ثم شغّل سير العمل `.github/workflows/build-onionmasq.yml` أو اتبع إعداداته.
4. ضع مكتبات ABI الناتجة في `app/src/main/jniLibs/<abi>/`.
5. نفّذ `gradle assembleDebug` لبناء نسخة التطوير.
6. بعد إعداد إعدادات الإصدار والتوقيع اللازمة، نفّذ `gradle assembleRelease bundleRelease` لبناء APK وAAB.
7. نجاح البناء لا يغني عن تثبيت التطبيق وإجراء اختبارات IP وDNS وIPv6 وKill Switch على جهاز حقيقي.

## الترخيص

لم يُضف إلى جذر هذا المستودع ملف ترخيص مستقل يحدد ترخيص التطبيق نفسه. يحتفظ مكوّن Onionmasq upstream بملفي الترخيص Apache-2.0 وMIT؛ راجع ملفات الترخيص في مصدره قبل إعادة التوزيع. لا تفترض أن ترخيص المكوّن يحدد تلقائيًا ترخيص كامل التطبيق.

## الالتزامات الرئيسية

- [إزالة الاعتماد على Orbot](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/d0eb4a752c364e9f2a0af4c5c285384132ebaf2b)
- [إضافة خدمة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/f73df844255d83026d82ea827721d6e050e052c0)
- [إضافة تدفق موافقة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/6547de59fbb48100bed3abed1ccb8670b14f56d3)
- [ربط Onionmasq بخدمة VPN](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/3c2fe3ff617de10987c57346f721970abc8e4e08)
- [ربط بناء APK ببناء المكتبات](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/81eccad11931a6ec6233e2c5387a3524bbc17b7f)
- [إصلاحات إعداد البناء](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/a0d346d6b6a73efc1a7129ad6de30d88e6557abb)
- [إصلاحات أداة Rust والبناء](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/1e92cd46382ff3302b7ee5bd6e4ab1a4b16d0ad4)
- [حفظ المكتبات الأصلية في المستودع](https://github.com/hisen7711ali-ctrl/Tor-phone/commit/11d90edf18ccfba07562b000a5ebb0ea5025e954)

## سير العمل

- [بناء مكتبات Onionmasq](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/workflows/build-onionmasq.yml)
- [بناء Android](https://github.com/hisen7711ali-ctrl/Tor-phone/actions/workflows/android-build.yml)


## دليل الاختبار وموارد الواجهة

- [دليل اختبار الهاتف الحقيقي](TEST.md): تثبيت نسخة الاختبار، موافقة VPN، فحص Tor وعنوان IP، واختبارات DNS وIPv6 وتوقف الخدمة وتبديل الشبكة وإعادة التشغيل.
- [رسم الأيقونة المتجهي](app/src/main/res/drawable/ic_launcher_foreground.xml) و[مصدر الرسم](tools/icon/ic_launcher.svg).
- [موارد النصوص الإنجليزية](app/src/main/res/values/strings.xml) و[موارد النصوص العربية](app/src/main/res/values-ar/strings.xml).
- [إعداد اللغات المدعومة](app/src/main/res/xml/locales_config.xml).
- [مقاييس المسافات والأحجام](app/src/main/res/values/dimens.xml) و[أنماط الخطوط](app/src/main/res/values/typography.xml).

اختيار اللغة محفوظ محليًا، ويستخدم إعداد لغات التطبيق في Android 13 وما بعده، وتطبيق إعدادات اللغة المحفوظة في الإصدارات الأقدم. حالة الاتصال المعروضة تأتي من رسائل خدمة VPN؛ وعند غياب حالة موثوقة تظهر حالة غير معروفة. لا تعني حالة تشغيل المحرك أن مرور البيانات عبر Tor قد ثبت.
