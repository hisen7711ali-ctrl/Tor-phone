# المساهمة في Tor-phone

أهلًا بالمساهمين. Tor-phone مشروع تجريبي يجمع Android VPN مع محرك Tor عبر Arti وOnionmasq. الأولوية لصحة التوجيه والخصوصية، ثم سهولة الاستخدام. نجاح البناء لا يعني أن الاتصال أو منع التسرب اجتاز اختبار هاتف.

## قبل فتح Issue أو Pull Request

1. ابحث في Issues وPull Requests المفتوحة لتجنب التكرار.
2. وضّح المشكلة والنتيجة المتوقعة وخطوات إعادة الإنتاج.
3. لا تنشر سجلات خام أو عناوين IP أو بيانات تصفح أو أسرار توقيع أو أسطر جسور خاصة.
4. ناقش التغييرات التي تمس حدود الأمان أو تدفق VPN قبل تنفيذها.

## قاعدة فصل المحرك عن الواجهة

**لا تقبل Pull Request واحدًا يغيّر محرك Rust وواجهة Android معًا.**

- تغييرات Rust/Onionmasq وملفات Cargo وJNI الأصلية تكون في Pull Request مستقل.
- تغييرات Java/Kotlin/Compose وموارد Android تكون في Pull Request مستقل.
- إذا احتاجت الميزة الطرفين، اقسمها إلى Pull Requests صغيرة ذات واجهة توافق موثقة، وادمج الجزء الأول قبل بناء الجزء الثاني فوقه.
- السبب: تقليل تعارضات الدمج، وتسهيل مراجعة الأمان، وعزل الأعطال عن مصدرها.
- لا تعدّل مصدر الوحدة الفرعية المثبتة بصمت؛ اشرح مصدر التغيير ورقم الالتزام وطريقة تحديث التثبيت.

## إعداد بيئة البناء

### المتطلبات

- Java 17.
- Android SDK 35 وAndroid Build Tools 35.0.0.
- Gradle 8.9.
- Android NDK r27d، الإصدار `27.3.13750724`.
- Rust 1.91.0.
- أهداف Rust: `aarch64-linux-android` و`armv7-linux-androideabi`.
- `cargo-ndk` الإصدار 3.5.4.
- استنساخ الوحدة الفرعية المثبتة.

### استنساخ المشروع

```bash
git clone --recurse-submodules https://github.com/hisen7711ali-ctrl/Tor-phone.git
cd Tor-phone
git submodule status
```

تحقق من أن الوحدة الفرعية على الالتزام المثبت في `third_party/onionmasq/VERSION` قبل البناء.

### بناء المكتبات الأصلية

من جذر المشروع شغّل سير العمل **Build Onionmasq native libraries** عبر GitHub Actions، أو جهّز NDK وRust والأهداف المذكورة أعلاه ثم نفّذ:

```bash
cd third_party/onionmasq/vendor
./build-ndk.sh arm64-v8a
./build-ndk.sh armeabi-v7a
```

يجب أن تُبنى المكتبات في وضع Release. لا ترفع مكتبات Debug أو ملفات غير مكتملة للتوزيع. يستخدم CI ملف إعداد البناء المثبت ويقيس أحجام المكتبات بعد إزالة الرموز.

### بناء تطبيق Android

من جذر المشروع:

```bash
gradle --no-daemon assembleDebug
```

لبناء حزم Release وAAB محليًا:

```bash
gradle --no-daemon assembleRelease bundleRelease
```

توقيع Release للنشر يتطلب مفتاحًا خاصًا بصاحب التطبيق. راجع [BUILD.md](BUILD.md)؛ لا تضع keystore أو كلمات مروره في المستودع.

## الاختبارات المطلوبة

### فحوص Rust

```bash
cd third_party/onionmasq/vendor
cargo fmt --check --package onionmasq-mobile
cargo check -p onionmasq-mobile --target aarch64-linux-android --release --features pluggable-transports
cargo check -p onionmasq-mobile --target armv7-linux-androideabi --release --features pluggable-transports
```

قد تتطلب أوامر الفحص إعداد NDK وبيئة الربط الخاصة بأهداف Android. إذا تعذر تشغيلها محليًا، استخدم سير CI ولا تدّع نجاح الفحص.

### فحوص Android

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

راجع كذلك [TEST_PLAN.md](TEST_PLAN.md) و[TEST.md](TEST.md). اختبارات الجهاز تشمل موافقة VPN، والتحقق من Tor، وتسرب DNS وIPv6، وتوقف التطبيق، وتبديل الشبكات، وإعادة التشغيل، والسكون، والبطارية.

**لا تكتب أن اختبار الهاتف نجح ما لم يُنفذ على هاتف فعلي وتُرفق نتيجة قابلة للمراجعة.**

## أسلوب الشيفرة

- اتبع تنسيق Rust الحالي وشغّل `cargo fmt --check`.
- استخدم أسماء واضحة، وتغييرات صغيرة، وتعليقات تشرح السبب لا ما تقوله الشيفرة حرفيًا.
- لا تضف سجلات تحتوي عناوين الوجهات أو محتوى المرور أو أسرار المستخدم.
- لا تستخدم `unwrap()` أو `expect()` في مسارات تشغيل حساسة جديدة دون مبرر ومعالجة واضحة للفشل.
- لا تعرض حالة «متصل» أو «محمي» اعتمادًا على ضغط زر أو حالة واجهة فقط؛ اربطها بإشارة مؤكدة من المصدر الفعلي.
- حافظ على التوافق مع ABI ‏`arm64-v8a` و`armeabi-v7a` ما لم يشرح التغيير خلاف ذلك.
- لا تغيّر إعدادات Release لتجاوز فشل الاختبارات أو تعطيل التحسينات دون سبب موثق.
- لا تضف ميزة تتطلب Root. Shizuku ليس Root ولا يجوز وصفه بذلك.

## وصف Pull Request

يجب أن يتضمن كل Pull Request:
- ملخص التغيير وسبب الحاجة إليه.
- الملفات والمكونات المتأثرة.
- الأوامر التي نُفذت ونتائجها الفعلية.
- ABI وإصدار Android عند وجود اختبار جهاز.
- الأثر الأمني والخصوصي والقيود المعروفة.
- تأكيد عدم تضمين أسرار أو سجلات شخصية.

## الترخيص

قبل إرسال مساهمة، راجع تراخيص المكونات الأصلية والاعتمادات. وجود ترخيص لـ Onionmasq لا يحدد تلقائيًا ترخيص التطبيق كاملًا.
