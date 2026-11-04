# المساهمة في Tor-phone

شكرًا لاهتمامك بالمساهمة. المشروع يتعامل مع توجيه الشبكة وخصوصية المستخدم؛ لذلك يجب أن تكون التغييرات صغيرة، قابلة للمراجعة، ومدعومة بدليل اختبار واضح. المشروع تجريبي، ولا يجوز وصف ميزة بأنها آمنة أو مكتملة لمجرد نجاح البناء.

## قبل البدء

1. افتح Issue واشرح المشكلة أو التغيير المقترح.
2. تحقق من عدم وجود عمل متزامن على الملفات نفسها.
3. ناقش التغييرات التي تمس مسار Tor أو VPN أو الصلاحيات قبل تنفيذها.
4. لا تضع مفاتيح أو كلمات مرور أو سجلات شخصية أو أسطر Bridges خاصة في المستودع.

## استنساخ المشروع

استنسخ المستودع مع الوحدة الفرعية:

```bash
git clone --recurse-submodules https://github.com/hisen7711ali-ctrl/Tor-phone.git
cd Tor-phone
```

إذا كان المستودع موجودًا لديك مسبقًا:

```bash
git submodule update --init --recursive
```

## متطلبات البناء

- Java 17.
- Android SDK 35 وAndroid Build Tools 35.0.0.
- Gradle 8.9.
- Android NDK r27d، الإصدار `27.3.13750724`.
- Rust 1.91.0.
- `cargo-ndk` الإصدار 3.5.4.
- أهداف Rust: `aarch64-linux-android` و`armv7-linux-androideabi`.

تفاصيل بناء المكتبات الأصلية موضحة في [BUILD.md](BUILD.md). لا تعدّل مرجع الوحدة الفرعية أو ملفات المصدر upstream دون توثيق سبب التغيير.

## بناء المكتبات الأصلية

يُنفذ بناء Android الأصلي من خلال سير العمل:

`Build Onionmasq native libraries`

يمكن تشغيله يدويًا من تبويب Actions بعد التأكد من تثبيت NDK وRust والأهداف المطلوبة. ينتج البناء مكتبات ABI منفصلة. يجب تسجيل أحجام الملفات قبل وبعد أي تحسين، وعدم استبدال مكتبات ABI بملف معماري غير مطابق.

## بناء التطبيق

بعد توفير المكتبات الأصلية:

```bash
gradle --no-daemon assembleDebug
```

ولإنشاء حزم Release/AAB محليًا:

```bash
gradle --no-daemon assembleRelease bundleRelease
```

قد تتطلب عملية Release مفتاح توقيع خاصًا. راجع قسم Release Signing في [BUILD.md](BUILD.md). لا تضع keystore داخل المستودع.

## الاختبارات

- نفّذ فحوص Rust المناسبة للمصدر الذي غيّرته، مثل `cargo fmt --all -- --check` و`cargo test` إذا كانت حزم workspace تدعمها.
- نفّذ بناء Android للمعماريات المتأثرة.
- اختبر على جهاز حقيقي عند تغيير مسار VPN أو Tor أو DNS أو Bridges أو دورة حياة الخدمة.
- راجع [TEST_PLAN.md](TEST_PLAN.md) و[TEST.md](TEST.md).
- وثّق الجهاز وإصدار Android وABI وخطوات إعادة الإنتاج والنتائج الفعلية.
- لا تكتب «نجح» إذا لم يُنفذ الاختبار. اكتب «لم يُختبر» أو «غير قابل للاختبار» عند الحاجة.

## أسلوب الكود والمراجعة

- اتبع تنسيق Rust الحالي، واستخدم `rustfmt` دون تغييرات تنسيق واسعة غير مرتبطة.
- حافظ على أسماء واضحة وأخطاء قابلة للتشخيص، وتجنب تسجيل بيانات المرور أو الأسرار.
- لا تضف اعتمادًا جديدًا أو ميزة Cargo دون توضيح أثرها على الحجم وسلسلة التوريد.
- لا تدّعِ أن تغييرًا في مسارات الشبكة يمنع التسرب قبل اختبارات الجهاز.
- حدّث الوثائق وسجل التغييرات عند تغيير السلوك أو حدود الخصوصية.
- اجعل كل Pull Request مركزًا على هدف واحد، مع وصف الملفات والاختبارات والقيود.

## قاعدة فصل المحرك عن الواجهة

**لا تُقبل Pull Requests تجمع في الوقت نفسه تغييرات محرك Rust وتغييرات واجهة Android (Java/Kotlin/Compose).** افصلها إلى طلبين مستقلين لتقليل تعارضات الدمج وتسهيل مراجعة المخاطر. إذا كان الربط بينهما ضروريًا، نسّق واجهة التكامل أولًا، ثم قدّم كل جانب في PR منفصل.

## Pull Request checklist

- [ ] شرحت المشكلة والسبب.
- [ ] حددت الملفات المتأثرة ولم أخلط تغييرات المحرك والواجهة في PR واحد.
- [ ] لم أضف أسرارًا أو بيانات مستخدم.
- [ ] نفذت الفحوص والبناء المناسبين أو شرحت لماذا تعذر ذلك.
- [ ] أرفقت نتائج الاختبار الفعلية، وميّزت غير المختبر.
- [ ] حدثت الوثائق عند الحاجة.
- [ ] راجعت حجم مكتبات `.so` وAPK إذا تأثرت التبعيات أو إعدادات البناء.

## English summary

Contributions should be small, reviewed, and backed by reproducible evidence. Clone with submodules, use the pinned Java/Android SDK/NDK/Rust toolchain documented in BUILD.md, run relevant Rust checks and Android builds, and test network behavior on real devices when applicable. Never commit keystores, credentials, private bridge lines, or personal logs.

**Do not submit a single PR that changes both the Rust engine and Android UI (Java/Kotlin/Compose).** Split those concerns into separate PRs to reduce merge conflicts and make security review tractable. Never claim a feature is secure or fully working based on a successful build alone.
