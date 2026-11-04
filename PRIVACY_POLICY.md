# سياسة الخصوصية — Tor-phone

**آخر تحديث: 4 نوفمبر 2026**

## العربية

### نطاق السياسة والحالة

Tor-phone مشروع تجريبي. تصف هذه الوثيقة سلوك التطبيق المعروف في المستودع حتى تاريخ التحديث، ولا تمثل تدقيقًا مستقلًا أو ضمانًا أمنيًا. الميزات التي لم تُفعّل بعد موضحة صراحةً أدناه.

### البيانات التي نجمعها

لا يجمع مطورو Tor-phone بيانات شخصية عبر خادم تابع للمشروع، ولا يرسل التطبيق إلى المطورين قاعدة بيانات استخدام أو ملفًا شخصيًا للمستخدم. لا توجد في المشروع حاليًا أدوات تحليلات أو تتبع استخدام أو قياس عن بُعد (Telemetry).

### ما لا نجمعه

- لا توجد تحليلات إعلانية أو معرّفات إعلانية مضافة من المشروع.
- لا توجد خدمة Telemetry ترسل إحصاءات استخدام إلى المطورين.
- لا نسجل سجل المواقع التي يزورها المستخدم.
- لا ينشئ التطبيق سجلًا لدى المطورين بعناوين IP الوجهات التي يطلبها المستخدم عبر دوائر Tor.
- لا نطلب من المستخدم إرسال سجل التصفح أو محتوى حركة الشبكة.

### اتصالات التحقق الخارجية

يتضمن المشروع مسارًا للتحقق من عنوان الخروج عبر خدمة HTTPS التابعة لـ `check.torproject.org`. عند طلب هذا الفحص، يتصل التطبيق بالخدمة الخارجية؛ وقد تتلقى الخدمة عنوان IP المصدر الذي يظهر لها وبيانات الاتصال الاعتيادية اللازمة لتقديم الرد. هذه الخدمة ليست خادمًا تابعًا لمطوري Tor-phone، وتخضع لسياسة مشغلها. لا يعني ذلك أن المطورين يستلمون عنوان IP أو يحتفظون به.

### دوائر Tor وعناوين الوجهات

لا يرسل التطبيق إلى المطورين سجلًا بعناوين IP الوجهات أو المواقع التي يزورها المستخدم. يعتمد مرور الشبكة على محرك Tor المدمج وإعدادات التطبيق والنظام. لا نقدم وعدًا بأن كل تطبيق أو كل نوع اتصال يمر عبر Tor؛ يجب مراجعة إعدادات Split Tunneling وإجراء اختبارات الجهاز الفعلية.

### تقسيم النفق (Split Tunneling)

يمكن للمستخدم اختيار تطبيقات مستثناة من نفق Tor. حركة التطبيقات المستثناة لا تمر عبر نفق Tor الذي ينشئه Tor-phone، ويكون هذا الاختيار محليًا من إعداد المستخدم. يجب عدم اعتبار التطبيقات المستثناة محمية بواسطة Tor-phone.

### الجسور (Bridges)

ميزة إدخال الجسور وربطها بالتطبيق ما زالت قيد التطوير ولم تُعتمد كميزة جاهزة في الإصدار الحالي. التصميم المقصود هو أن أسطر الجسور التي يقدمها المستخدم تُحفظ محليًا على الجهاز فقط، ولا تُرسل إلى خوادم المطورين. لا تُدخل بيانات حساسة في نسخة تجريبية قبل التحقق من تنفيذ التخزين المحلي وسلوكه على جهازك.

### السجلات والأذونات

قد ينتج Android أو أدوات التطوير سجلات نظام عند حدوث أخطاء. لا تُرسل هذه السجلات تلقائيًا إلى المطورين من خلال نظام Telemetry في المشروع. إذا شارك المستخدم سجلًا يدويًا في Issue، فعليه إزالة عناوين IP والمعرّفات وأي معلومات شخصية أولًا.

### التواصل بشأن الخصوصية

للاستفسارات أو الإبلاغ عن مشكلة خصوصية، افتح Issue في مستودع المشروع:
https://github.com/hisen7711ali-ctrl/Tor-phone/issues

لا ترفق سجلات خام أو بيانات تصفح أو أسرارًا أو أسطر جسور خاصة في البلاغ.

---

## English

**Last updated: 4 November 2026**

### Scope and status

Tor-phone is an experimental project. This notice describes the behavior visible in the repository as of the date above; it is not an independent security audit or a security guarantee. Features that are not yet available are identified below.

### Data we collect

The Tor-phone developers do not operate a project backend that collects users' personal data. The app does not send an analytics database, usage profile, or telemetry feed to the developers. The project currently includes no analytics or usage telemetry.

### Data we do not collect

- No project-added advertising analytics or advertising identifiers.
- No telemetry reporting usage statistics to the developers.
- No developer-maintained history of websites visited.
- No developer-side log of destination IP addresses requested through Tor circuits.
- No requirement to send browsing history or traffic contents to the developers.

### External verification requests

The project includes an HTTPS exit-IP verification path using `check.torproject.org`. When a user requests this check, the app contacts that external service. The service may receive the source IP address visible to it and ordinary connection metadata needed to respond. It is not a Tor-phone developer-operated server and is governed by its operator's policy. This does not mean the developers receive or retain that IP address.

### Tor circuits and destination addresses

The app does not send developers a log of destination IP addresses or websites visited. Network routing depends on the embedded Tor engine and the app and Android configuration. We do not claim that every app or every connection type is routed through Tor; review Split Tunneling settings and test the actual device.

### Split Tunneling

Users can select apps to exclude from the Tor VPN tunnel. Traffic from excluded apps does not use the Tor-phone tunnel. The selection is a local user setting. Excluded apps must not be considered protected by Tor-phone.

### Bridges

Bridge entry and app integration are still under development and are not a released, verified feature in the current version. The intended design is that user-provided bridge lines are stored locally on the device only and are not sent to developer servers. Do not enter sensitive bridge data in a test build until local storage behavior has been verified on your device.

### Logs and permissions

Android or development tools may produce system logs when errors occur. The project does not automatically upload those logs to developers through telemetry. If a user manually shares logs in an Issue, they should remove IP addresses, identifiers, and personal information first.

### Privacy contact

For privacy questions or to report a privacy issue, open an Issue in the project repository:
https://github.com/hisen7711ali-ctrl/Tor-phone/issues

Do not attach raw logs, browsing data, secrets, or private bridge lines.
