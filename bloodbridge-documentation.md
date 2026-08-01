# توثيق مشروع BloodBridge - منصة إدارة التبرع بالدم

## 1. نظرة عامة على المشروع

**BloodBridge** هو نظام متكامل لإدارة التبرع بالدم، تم بناؤه باستخدام **Spring Boot**، وهو بمثابة إعادة بناء (Port) لنظام موجود مسبقًا مبني بـ Laravel. المنصة تهدف إلى ربط المتبرعين بالدم مع المؤسسات الصحية (مستشفيات، بنوك دم، مراكز صحية) لتلبية طلبات التبرع بالدم بشكل ذكي وفعال.

| المواصفة | القيمة |
|----------|--------|
| **اسم المشروع** | BloodBridge |
| **الإصدار** | 1.0.0-SNAPSHOT |
| **Spring Boot** | 3.3.2 |
| **Java** | 21 |
| **قاعدة البيانات** | MySQL 8.0 |
| **أداة الترحيل** | Flyway |
| **الأمان** | Spring Security 6 + JWT |
| **التوثيق** | Swagger (SpringDoc OpenAPI 2.6.0) |

---

## 2. هيكل الحزم (Package Structure)

```
com.bloodbridge.bloodbridge
├── BloodBridgeApplication.java          # نقطة الدخول الرئيسية
├── config/                              # إعدادات المشروع
├── controller/                          # طبقة الـ API
├── dto/                                 # كائنات نقل البيانات
├── entity/                              # كيانات قاعدة البيانات
├── enumtype/                            # الأنواع المحددة (Enums)
├── exception/                           # معالجة الأخطاء
├── job/                                 # المهام غير المتزامنة
├── jwt/                                 # إدارة JWT
├── notification/                        # أنواع الإشعارات
├── repository/                          # طبقة الوصول للبيانات
├── schedule/                            # المهام المجدولة
├── service/                             # منطق الأعمال
│   └── scoring/                         # نظام تسجيل وتقييم المتبرعين
└── util/                                # أدوات مساعدة
```

---

## 3. قاعدة البيانات والكيانات (Entities)

### 3.1 جدول المستخدمين (`users`)

يمثل هذا الجدول المستخدمين بجميع أدوارهم (متبرع، مؤسسة، مشرف).

| الحقل | النوع | وصف |
|-------|------|------|
| `id` | Long (PK) | المعرف الفريد |
| `name` | String | اسم المستخدم |
| `email` | String (Unique) | البريد الإلكتروني |
| `password` | String | كلمة المرور (مشفرة بـ BCrypt) |
| `phone` | String | رقم الهاتف |
| `role` | UserRole | الدور: ADMIN, DONOR, ORGANIZATION |
| `is_active` | Boolean | حالة التفعيل |
| `email_verified_at` | DateTime | تاريخ تأكيد البريد |
| `locale` | String | اللغة (en/ar) |
| `deleted_at` | DateTime | للحذف الناعم (Soft Delete) |

### 3.2 جدول المتبرعين (`donors`)

| الحقل | النوع | وصف |
|-------|------|------|
| `id` | Long (PK) | المعرف الفريد |
| `user_id` | Long (FK) | رابط لجدول users |
| `governorate_id` | Long (FK) | المحافظة |
| `national_id` | String (9 chars) | الرقم الوطني |
| `gender` | Gender | ذكر/أنثى |
| `birth_date` | LocalDate | تاريخ الميلاد |
| `lat`, `lng` | Double | الإحداثيات (GPS) |
| `points` | Integer | نقاط التبرع |
| `level` | Integer | المستوى |

### 3.3 جدول الملف الصحي (`donor_health_profiles`)

| الحقل | النوع | وصف |
|-------|------|------|
| `id` | Long (PK) | المعرف الفريد |
| `donor_id` | Long (FK, Unique) | رابط المتبرع |
| `weight` | Integer | الوزن (كجم) |
| `height` | Integer | الطول (سم) |
| `chronic_disease` | Boolean | أمراض مزمنة |
| `blood_type` | BloodType | فصيلة الدم |
| `verified_blood_type` | BloodType | فصيلة الدم المؤكدة مخبريًا |
| `total_donations` | Integer | إجمالي التبرعات |
| `is_eligible` | Boolean | أهلية التبرع |

### 3.4 جدول طلبات الدم (`blood_requests`)

| الحقل | النوع | وصف |
|-------|------|------|
| `id` | Long (PK) | المعرف الفريد |
| `organization_id` | Long (FK) | رابط المؤسسة |
| `blood_type` | BloodType | فصيلة الدم المطلوبة |
| `units_needed` | Integer | عدد الوحدات المطلوبة |
| `urgency_level` | UrgencyLevel | NORMAL أو CRITICAL |
| `search_radius_km` | Integer | نصف قطر البحث (كم) |
| `lat`, `lng` | Double | موقع الطلب |
| `status` | BloodRequestStatus | PENDING, BROADCASTED, FULFILLED, CANCELLED, EXPIRED |

### 3.5 جدول استجابات المتبرعين (`request_responses`)

| الحقل | النوع | وصف |
|-------|------|------|
| `id` | Long (PK) | المعرف الفريد |
| `blood_request_id` | Long (FK) | رابط طلب الدم |
| `donor_id` | Long (FK) | رابط المتبرع |
| `status` | RequestResponseStatus | حالة الاستجابة |
| `verification_qr_code` | String (64 chars) | كود QR للتحقق |
| `qr_code_expires_at` | DateTime | تاريخ انتهاء صلاحية QR |
| `distance` | Float | المسافة عن موقع الطلب |
| `decline_reason` | String | سبب الرفض |

**حالات الاستجابة:**
- `PENDING` - في انتظار الرد
- `ACCEPTED` - تم القبول
- `DECLINED` - تم الرفض
- `COMPLETED` - تم التبرع
- `IGNORED` - تم التجاهل
- `NO_SHOW` - لم يحضر
- `UNREACHABLE` - غير متاح
- `NOT_NEEDED` - لم تعد هناك حاجة

### 3.6 باقي الجداول

| الجدول | الوصف |
|--------|-------|
| `organizations` | المؤسسات الصحية مع بياناتها (الترخيص، الموقع، ساعات العمل) |
| `governorates` | المحافظات (خمس محافظات في قطاع غزة) |
| `appointments` | مواعيد التبرع (مجدول، مؤكد، مكتمل، ملغي) |
| `notifications` | الإشعارات (مخزنة بصيغة JSON) |
| `achievements` | الإنجازات/الشارات (نظام المكافآت) |
| `donor_achievements` | ربط المتبرعين بالإنجازات |
| `eligibility_logs` | سجل فحوصات الأهلية |
| `donor_predictive_scores` | درجات التنبؤ بالتبرع (من AI) |
| `model_training_logs` | سجل تدريب نماذج AI |
| `settings` | الإعدادات العامة للنظام (قابلة للتعديل) |
| `contact_messages` | رسائل التواصل |
| `announcements` | الإعلانات (بالعربية والإنجليزية) |

---

## 4. نقط النهاية (API Endpoints) - كلها تحت `/api/v1/`

### 4.1 المصادقة (`/api/v1/auth`) - عامة

| الطريقة | المسار | الوصف |
|---------|--------|-------|
| POST | `/register` | تسجيل مستخدم جديد |
| POST | `/login` | تسجيل الدخول |
| POST | `/refresh` | تجديد رمز JWT |

### 4.2 المتبرع (`/api/v1/donor`) - يتطلب دور DONOR

| الطريقة | المسار | الوصف |
|---------|--------|-------|
| GET | `/blood-requests` | عرض طلبات الدم المتاحة |
| GET | `/blood-requests/{id}` | تفاصيل طلب معين |
| POST | `/blood-requests/{id}/accept` | قبول طلب التبرع |
| POST | `/blood-requests/{id}/decline` | رفض طلب التبرع |
| POST | `/blood-requests/{id}/ignore` | تجاهل طلب التبرع |
| GET | `/responses` | سجل استجابات المتبرع |
| GET | `/responses/{id}/qr/download` | تحميل كود QR |
| GET | `/achievements` | إنجازات المتبرع |

### 4.3 المؤسسة (`/api/v1/org`) - يتطلب دور ORGANIZATION

| الطريقة | المسار | الوصف |
|---------|--------|-------|
| GET | `/profile` | عرض بيانات المؤسسة |
| POST | `/blood-requests` | إنشاء طلب دم وبثه |
| GET | `/blood-requests` | قائمة طلبات المؤسسة |
| GET | `/blood-requests/{id}` | تفاصيل طلب |
| POST | `/blood-requests/{id}/broadcast` | إعادة بث طلب |
| GET | `/blood-requests/{id}/responses` | استجابات طلب معين |
| GET | `/responses` | كل استجابات المؤسسة |
| POST | `/scan-qr` | فحص كود QR (محدود بـ 30 مسح/دقيقة) |
| POST | `/responses/{id}/complete` | تأكيد إتمام التبرع |

### 4.4 المشرف (`/api/v1/admin`) - يتطلب دور ADMIN

| الطريقة | المسار | الوصف |
|---------|--------|-------|
| GET | `/users` | قائمة المستخدمين |
| GET | `/users/{id}` | تفاصيل مستخدم |
| PUT | `/users/{id}` | تحديث مستخدم |
| GET | `/donors` | قائمة المتبرعين |
| GET | `/organizations` | قائمة المؤسسات |
| PUT | `/organizations/{id}/approve` | الموافقة على مؤسسة |
| PUT | `/organizations/{id}/reject` | رفض مؤسسة |
| GET `/PUT` | `/blood-requests/*` | إدارة طلبات الدم |
| GET `/POST` | `/achievements` | إدارة الإنجازات |
| GET `/POST` | `/announcements` | إدارة الإعلانات |
| GET | `/contact-messages` | رسائل التواصل |

---

## 5. طبقة الأمان (Security)

### 5.1 Spring Security + JWT

- **نظام المصادقة**: JWT (JSON Web Token) مع توقيع HMAC-SHA
- **مدة صلاحية التوكن**: 24 ساعة للوصول، 7 أيام للتحديث
- **جلسات عديمة الحالة**: `SessionCreationPolicy.STATELESS`
- **CORS**: يسمح بجميع المصادر والأساليب
- **CSRF**: معطل (مناسب لـ REST API)

### 5.2 توجيه الأدوار (Role-based Routing)

```
/auth/**      → عام (بدون مصادقة)
/admin/**     → ADMIN فقط
/donor/**     → DONOR فقط
/org/**       → ORGANIZATION فقط
/swagger-ui/** → عام
/actuator/**  → عام
كل ما عدا ذلك  → يتطلب مصادقة
```

### 5.3 المعترضات (Interceptors)

هذه معترضات تعمل على مستوى الـ HTTP Request (مثل Middleware):

1. **SyncUserLocaleInterceptor** - يضبط لغة المستخدم تلقائيًا
2. **EmailVerificationInterceptor** - يمنع المستخدمين غير المؤكدين بريدهم
3. **DonorIneligibilityInterceptor** - يمنع المتبرعين غير المؤهلين (مرض مزمن)
4. **OrganizationApprovalInterceptor** - يمنع المؤسسات غير المعتمدة

---

## 6. الخدمات الرئيسية (Services)

### 6.1 AuthService - المصادقة

- **تسجيل مستخدم جديد**: التحقق من تطابق كلمات المرور، عدم تكرار البريد، إنشاء المستخدم، إرجاع JWT
- **تسجيل الدخول**: التحقق من صحة البيانات، إرجاع زوج التوكنات (access + refresh)
- **تحديث التوكن**: استخدام refresh token ساري المفعول لإصدار زوج جديد

### 6.2 BloodRequestBroadcastService - بث طلبات الدم

هذه هي الخدمة الأكثر تعقيدًا في المشروع، وتقوم بالتالي:

1. **التحقق من موقع الطلب**: التأكد من وجود إحداثيات صالحة
2. **البحث التدريجي عن المتبرعين**: تبدأ بـ `searchRadiusKm` الأولي وتتوسع بمقدار 5 كم في كل مرة حتى 25 كم كحد أقصى
3. **تعديل محسّن للطوارئ**: في حالة الطوارئ (CRITICAL)، يتم ضرب نصف القطر الأولي في 3
4. **التراجع للفصائل غير المعروفة**: إذا لم يتم العثور على متبرعين بالفصيلة المطلوبة، يتم البحث عن متبرعين بفصيلة UNKNOWN
5. **تسجيل المتبرعين وتقييمهم**: يستخدم نظام التسجيل الرباعي المستوى لإختيار أفضل المتبرعين
6. **إنشاء استجابات معلقة**: لكل متبرع مختار، يتم إنشاء سجل `RequestResponse` بحالة PENDING
7. **إرسال الإشعارات**: عبر WebSocket و قاعدة البيانات

### 6.3 DonorScoringService - نظام تسجيل المتبرعين

نظام متعدد المستويات لتقييم المتبرعين وإختيار الأفضل:

#### المستوى 1: التخزين المؤقت (Cache DB)
إذا كانت درجة المتبرع محسوبة مسبقًا وأقل من 24 ساعة، يتم استخدامها مباشرة.

#### المستوى 2: FastAPI AI
يتم استدعاء خدمة تعلم آلة خارجية (FastAPI) لحساب احتمالية قبول المتبرع. هذا المستوى محمي بـ **Circuit Breaker** (قاطع الدائرة) - إذا فشل 3 مرات متتالية، يتم إيقاف المحاولات لمدة 120 ثانية.

#### المستوى 3: القواعد الثابتة (Rule-based)
إذا فشلت المستويات السابقة، يتم حساب الدرجة بناءً على:
- **معدل القبول**: 50% - عدد المرات التي قبل فيها المتبرع
- **الحداثة**: 30% - مدى حداثة آخر استجابة
- **الولاء**: 20% - إجمالي مرات التبرع

#### المستوى 4: الإختيار بـ Epsilon-Greedy
- المتبرعون الجدد (أقل من 3 استجابات) يدخلون في **مجموعة الاستكشاف**
- أصحاب الدرجات العالية يدخلون في **مجموعة الاستغلال**
- يتم ضمان نسبة استكشاف دنيا لا تقل عن 10%

### 6.4 BloodRequestActionService - إجراءات طلبات الدم

- **قبول طلب**: التحقق من الأهلية، الحد الأقصى للطلبات النشطة (3)، إنشاء كود QR للمتبرع
- **رفض/تجاهل**: تسجيل السبب إن وجد
- **إلغاء الطلبات الزائدة**: تعليم الاستجابات المعلقة كـ NOT_NEEDED
- **إتمام التبرع**: تأكيد عملية التبرع

### 6.5 DonorEligibilityService - أهلية التبرع

تحسب أهلية المتبرع بناءً على شروط قابلة للتعديل من قاعدة البيانات:

| الشرط | الحد الأدنى |
|-------|------------|
| الوزن | 50 كجم |
| الطول | 140 سم |
| المدة بين التبرعات | 90 يومًا |
| انتظار بعد العدوى | 14 يومًا |
| انتظار بعد الجراحة | 28 يومًا |

### 6.6 NotificationService - الإشعارات

- تخزين الإشعارات في جدول `notifications` بصيغة JSON
- دعم الإشعارات بلغة المستخدم (عربي/إنجليزي)
- 5 أنواع من الإشعارات:
  - **BloodRequestMatchNotification** - تطابق طلب دم مع متبرع
  - **DonorIneligibilityNotification** - إبلاغ المتبرع بعدم الأهلية
  - **DonorResponseNotification** - إبلاغ المؤسسة باستجابة متبرع
  - **ResponseNotNeededNotification** - إبلاغ المتبرع بعدم الحاجة
  - **SystemAnnouncementNotification** - إعلانات النظام

### 6.7 QRCodeService - أكواد QR

- توليد رمز سداسي عشري عشوائي (16 بايت = 32 حرفًا) مرتبط بكل استجابة
- إنشاء صورة QR بصيغة PNG باستخدام مكتبة ZXing
- صلاحية محدودة للرمز (قابلة للتكوين)
- التحقق من الرابط: التأكد من أن QR يخص المؤسسة الماسحة

### 6.8 RateLimitService - الحد من الطلبات

تطبيق **نافذة زمنية منزلقة (Sliding Window)** لمنع الإساءة:

| العملية | الحد الأقصى |
|---------|-------------|
| مسح QR | 30 مرة/دقيقة |
| إرسال رسائل تواصل | 3 مرات/دقيقة |
| تأكيد البريد | 6 مرات/دقيقة |

---

## 7. المهام المجدولة (Scheduled Jobs)

| المهمة | الجدول | الوصف |
|--------|--------|-------|
| `cleanupStaleResponses` | كل ساعة | تحويل الاستجابات المعلقة منذ 8 ساعات (طوارئ) أو 48 ساعة (عادي) إلى UNREACHABLE |
| `expireOldBloodRequests` | كل 12 ساعة | إنهاء طلبات الدم المنتهية (أكثر من 48 ساعة) |
| `decayEpsilon` | كل أسبوع (الإثنين) | تخفيض معدل الاستكشاف إلى النصف (الحد الأدنى 0.01) |

---

## 8. المهام غير المتزامنة (Async Jobs)

- **DispatchBloodRequestNotifications** - إرسال إشعارات مجمعة بعد بث طلب
- **CancelExcessResponsesJob** - إلغاء الاستجابات الزائدة بعد وصول العدد المطلوب من المتبرعين

---

## 9. WebSocket

- **نقطة الاتصال**: `/ws` (مع SockJS للتوافق مع المتصفحات القديمة)
- **الوسطاء**: `/topic` (عام)، `/queue` (خاص)، `/user` (شخصي)
- **بادئة التطبيق**: `/app`
- يستخدم STOMP protocol فوق WebSocket

---

## 10. التكامل مع FastAPI AI Service

يتكامل المشروع مع خدمة تعلم آلة مبنية بـ FastAPI على المنفذ 8001:

| المسار | الوظيفة |
|--------|---------|
| `POST /api/score` | حساب احتمالية قبول المتبرع |
| `POST /api/retrain` | إعادة تدريب النموذج |
| `GET /api/health` | فحص صحة الخدمة |

يتم الاتصال عبر WebClient مع **WebClient Circuit Breaker** (3 محاولات فاشلة → فتح الدائرة لمدة 120 ثانية → نصف فتح → إغلاق).

---

## 11. الإعدادات (Settings)

نظام إعدادات ديناميكي مخزن في قاعدة البيانات مدعوم بالتخزين المؤقت:

```
الإعدادات العامة (8):
- app_name, support_email, max_donations_per_day, إلخ

إعدادات التسجيل (7):
- acceptance_weight, loyalty_weight, recency_weight, epsilon_value, إلخ

إعدادات الأهلية:
- min_weight, min_height, donation_interval_days, إلخ
```

كل الإعدادات قابلة للتعديل من لوحة التحكم وتحديث الذاكرة المؤقتة تلقائيًا.

---

## 12. Flyway Migrations

- **V1**: إنشاء 23 جدولًا (الهيكل الأساسي)
- **V2**: البيانات الأولية (5 محافظات، 6 إنجازات، 7 إعدادات تسجيل، 8 إعدادات عامة)

---

## 13. Docker

- **Dockerfile متعدد المراحل**: يستخدم `eclipse-temurin:21`، بناء Maven، تشغيل بصورة JRE مع مستخدم غير جذر
- **Docker Compose**: 3 خدمات (MySQL 8.0 + Spring Boot + FastAPI AI)
- المنفذ 8080 للتطبيق

---

## 14. الاختبارات (Tests)

- **6 اختبارات خدمات**:
  - `BloodRequestActionServiceTest`
  - `BloodRequestBroadcastServiceTest`
  - `DonorEligibilityServiceTest`
  - `NotificationServiceTest`
  - `QRCodeServiceTest`
  - `RateLimitServiceTest`

- **فئة أساسية** `AbstractIntegrationTest` مع Testcontainers MySQL
- **JaCoCo** لتغطية الكود

---

## 15. آلية عمل النظام (سيناريو كامل)

### سيناريو: طلب دم من مؤسسة

```
1. المؤسسة ترسل POST /api/v1/org/blood-requests
   → { bloodType, unitsNeeded, lat, lng, urgencyLevel }

2. BloodRequestBroadcastService.broadcast():
   a. التحقق من موقع الطلب
   b. البحث عن متبرعين متوافقين بفصيلة الدم في نطاق searchRadiusKm
   c. إذا لم يتم العثور على عدد كافٍ، توسيع النطاق بـ +5 كم (حتى 25 كم)
   d. إذا كانت الحالة CRITICAL، البحث يبدأ بـ radius × 3
   e. إذا لم يتم العثور على أحد، التراجع للفصيلة UNKNOWN
   f. المتبرعون الموجودون يتم تقييمهم:
      - المستوى 1: التحقق من التخزين المؤقت
      - المستوى 2: استدعاء FastAPI AI (مع Circuit Breaker)
      - المستوى 3: حساب القواعد الثابتة
   g. تطبيق Epsilon-Greedy (استكشاف vs استغلال)
   h. اختيار أفضل المتبرعين وحفظ استجابات PENDING
   i. إرسال إشعارات WebSocket للمتبرعين

3. المتبرع يستلم الإشعار ويضغط "قبول"
   → POST /api/v1/donor/blood-requests/{id}/accept

4. BloodRequestActionService.accept():
   a. التحقق من أهلية المتبرع (وزن، طول، مدة)
   b. التحقق من عدد الطلبات النشطة (حد أقصى 3)
   c. توليد كود QR فريد
   d. حساب المسافة بين المتبرع وموقع الطلب
   e. إرسال إشعار للمؤسسة

5. المؤسسة تفحص QR عند حضور المتبرع
   → POST /api/v1/org/scan-qr (مقيد بـ 30 مرة/دقيقة)

6. QRCodeService يتحقق من:
   a. صحة التوقيع
   b. صلاحية الكود (لم ينته)
   c. أن الكود يخص هذه المؤسسة

7. المؤسسة تؤكد إتمام التبرع
   → POST /api/v1/org/responses/{id}/complete

8. الإجراءات اللاحقة:
   a. تحديث DonorHealthProfile.totalDonations
   b. منح نقاط للمتبرع
   c. التحقق من الإنجازات
   d. إلغاء الاستجابات المعلقة الأخرى لهذا الطلب
```

---

## 16. التقنيات والمكتبات المستخدمة

| المكتبة | الإصدار | الاستخدام |
|---------|---------|-----------|
| Spring Boot Starter Web | 3.3.2 | REST API |
| Spring Data JPA | 3.3.2 | ORM |
| Spring Security | 6.x | المصادقة والصلاحيات |
| Spring Validation | 3.3.2 | التحقق من صحة البيانات |
| Spring Mail | 3.3.2 | إرسال البريد الإلكتروني |
| Spring WebSocket | 3.3.2 | الإشعارات اللحظية |
| Spring Cache | 3.3.2 | التخزين المؤقت |
| Spring Actuator | 3.3.2 | المراقبة |
| Flyway | ? | ترحيل قاعدة البيانات |
| JJWT | 0.12.6 | JWT tokens |
| Lombok | 1.18.34 | تقليل الكود المتكرر |
| MapStruct | 1.5.5.Final | تحويل DTO ↔ Entity |
| ZXing | 3.5.2 | توليد QR codes |
| Quartz Scheduler | ? | المهام المجدولة |
| FreeMarker | ? | القوالب (i18n) |
| Testcontainers MySQL | ? | اختبارات التكامل |
| H2 | ? | اختبارات |
| JaCoCo | 0.8.12 | تغطية الاختبارات |
| SpringDoc OpenAPI | 2.6.0 | توثيق API |
| Micrometer Prometheus | ? | مراقبة الأداء |

---

## 17. ملخص التصميم (Architecture)

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│  Spring Boot  │────▶│   MySQL DB  │
│ (Web/Mobile)│     │  Application  │     │             │
└─────────────┘     │              │     └─────────────┘
                    │  ┌──────────┐│
                    │  │ Security ││     ┌─────────────┐
                    │  │  JWT     ││────▶│  FastAPI AI │
                    │  └──────────┘│     │  Service    │
                    │              │     └─────────────┘
                    │  ┌──────────┐│
                    │  │ WebSocket││────▶ Real-time
                    │  └──────────┘│     Notifications
                    │              │
                    │  ┌──────────┐│
                    │  │ Quartz   ││────▶ Scheduled
                    │  │ Jobs     ││     Tasks
                    │  └──────────┘│
                    └──────────────┘
```

### النمط المعماري:
- **Layered Architecture** (Controller → Service → Repository)
- **DTO Pattern** لفصل طبقة API عن الكيانات
- **Interceptor Pattern** للمعالجة الوسيطة
- **Strategy Pattern** في نظام التسجيل (4 مستويات)
- **Circuit Breaker Pattern** في الاتصال بـ FastAPI
- **Rate Limiting** بـ Sliding Window
- **WebSocket + STOMP** للإشعارات اللحظية
- **Flyway** لإدارة ترقيات قاعدة البيانات

---

## 18. تحسينات البنية التحتية المتقدمة (Infrastructure Upgrades)

### 18.1 الهيكل الأحادي المعياري (Modular Monolith)

تم إعادة تنظيم المشروع إلى **نموذج الوحدة الأحادية المعيارية** لفصل المسؤوليات بشكل أفضل دون تعقيد إضافي:

```
com.bloodbridge.bloodbridge
├── shared/              # مشترك بين كل الوحدات
│   ├── domain/          # أحداث المجال، Redis، Resilience4j، Micrometer
│   ├── outbox/          # نمط Outbox
│   ├── audit/           # سجل التدقيق
│   └── idempotency/     # منع التكرار
├── auth/                # وحدة المصادقة
├── donor/               # وحدة المتبرع  
├── organization/        # وحدة المؤسسة
├── bloodrequest/        # وحدة طلبات الدم
│   └── domain/          # أحداث المجال الخاصة بطلبات الدم
└── notification/        # وحدة الإشعارات
```

كل وحدة تحتوي على منطق الأعمال الخاص بها (services, domain events) وتتواصل مع الوحدات الأخرى عبر **الأحداث** (Events) وليس الاستدعاء المباشر.

---

### 18.2 نمط Outbox (Transactional Outbox Pattern)

**الغرض**: ضمان إرسال الأحداث (إشعارات، WebSocket، تكامل خارجي) بشكل موثوق دون فقدان البيانات عند فشل الإرسال.

**كيف يعمل**:
```
1. الخدمة تنفذ عملية (مثل بث طلب دم) داخل transaction واحدة
2. يتم حفظ الحدث في جدول outbox_events (في نفس transaction)
3. OutboxProcessor - مهمة مجدولة تعمل كل 30 ثانية:
   a. تبحث عن الأحداث المعلقة (PENDING)
   b. تحاول إرسالها (نشر الحدث)
   c. إذا نجحت → تحديث الحالة إلى COMPLETED
   d. إذا فشلت → تحديث الحالة إلى FAILED + تسجيل الخطأ
```

**المكونات**:

| المكون | الوظيفة |
|--------|---------|
| `OutboxEvent` | كيان JPA يمثل الحدث |
| `OutboxStatus` | حالات الحدث: PENDING, PROCESSING, COMPLETED, FAILED |
| `OutboxRepository` | الوصول لجدول outbox_events |
| `OutboxService` | حفظ + معالجة + تحديث الأحداث |
| `OutboxProcessor` | `@Scheduled(fixedRate = 30000)` - معالجة المعلقة كل 30 ثانية |

**مثال استخدام**:
```java
// داخل أي Service
outboxService.saveEvent(
    new BloodRequestBroadcastedEvent(requestId, orgId, donorCount, donorIds, radius),
    "BloodRequest",
    requestId
);
```

**الأحداث المدعومة حالياً**:
- `BloodRequestCreatedEvent` - عند إنشاء طلب دم
- `BloodRequestBroadcastedEvent` - عند بث طلب
- `DonationCompletedEvent` - عند إتمام تبرع
- `DonorAcceptedRequestEvent` - عند قبول متبرع لطلب
- `OrganizationApprovedEvent` - عند اعتماد مؤسسة

---

### 18.3 سجل التدقيق (Audit Log)

**الغرض**: تسجيل كل العمليات الهامة في النظام للأغراض الأمنية والرقابية.

**المكونات**:

| المكون | الوظيفة |
|--------|---------|
| `AuditLog` | كيان JPA مع حقول: event_type, actor_id, actor_type, resource_type, resource_id, action, details, old_values, new_values, ip_address, user_agent |
| `AuditLogRepository` | وصول لجدول audit_logs |
| `AuditLogService` | تسجيل الأحداث بشكل غير متزامن + استعلام عنها |

**مثال استخدام**:
```java
auditLogService.log(AuditLogEvent.builder()
    .eventType("BLOOD_REQUEST.BROADCAST")
    .actorId(user.getId())
    .actorType(user.getRole().name())
    .resourceType("BloodRequest")
    .resourceId(requestId)
    .action("BROADCAST")
    .details("Blood request broadcasted to " + donorCount + " donors")
    .build());
```

---

### 18.4 منع التكرار (Idempotency)

**الغرض**: حماية API من تنفيذ نفس الطلب أكثر من مرة عند إعادة الإرسال (network retry).

**كيف يعمل**:
```
1. العميل يرسل header: Idempotency-Key: <مفتاح فريد>
2. IdempotencyFilter يتحقق:
   - هل المفتاح موجود مسبقاً؟ → يعيد الاستجابة السابقة مباشرة
   - غير موجود؟ → ينفذ الطلب ويخزن الاستجابة للمفتاح
3. صلاحية المفتاح: 24 ساعة
```

**المكونات**:

| المكون | الوظيفة |
|--------|---------|
| `IdempotencyKey` | كيان JPA: idempotency_key, http_method, request_path, response_status, response_body, expires_at |
| `IdempotencyKeyRepository` | وصول لجدول idempotency_keys |
| `IdempotencyFilter` | `Filter` يتحقق من header قبل تنفيذ الطلب |
| `IdempotencyCleanup` | `@Scheduled(cron = "0 0 4 * * *")` - تنظيف المفاتيح المنتهية يومياً 4 صباحاً |

**الطلبات المدعومة**: POST, PUT, PATCH فقط.

---

### 18.5 Redis - التخزين المؤقت والحد من الطلبات

**الغرض**: تسريع الاستعلامات المتكررة ومنع إساءة الاستخدام.

**الإعدادات** (`application.yml`):
```yaml
bloodbridge:
  redis:
    enabled: true                          # تفعيل Redis
    jwt-blacklist-ttl-hours: 24            # مدة بقاء التوكن في القائمة السوداء
    jwt-blacklist-enabled: true            # تفعيل قائمة سوداء للتوكن
```

**مكونات Redis**:

| المكون | الوظيفة |
|--------|---------|
| `RedisConfig` | تهيئة `RedisTemplate` و `RedisCacheManager` مع TTL لكل cache |
| `RedisRateLimiter` | **نافذة زمنية منزلقة (Sliding Window)** عبر `SortedSet` |
| `JwtBlacklistFilter` | التحقق من التوكن في القائمة السوداء قبل كل طلب |

**الذاكرة المؤقتة (Caches)**:

| اسم Cache | TTL | الاستخدام |
|-----------|-----|-----------|
| `settings` | 24 ساعة | إعدادات النظام |
| `donors` | 30 دقيقة | بيانات المتبرعين |
| `bloodRequests` | 15 دقيقة | طلبات الدم |
| `governorates` | 7 أيام | المحافظات |

**شروط التفعيل**: جميع مكونات Redis مشروطة بـ `@ConditionalOnProperty(name = "bloodbridge.redis.enabled", havingValue = "true")` - إذا تم تعطيلها، يعمل النظام بشكل طبيعي بدون Redis.

---

### 18.6 Resilience4j - تحمل الأخطاء

**الغرض**: حماية النظام من الانهيار المتسلسل عند فشل الخدمات الخارجية.

**المكونات**:

| المكون | الإعدادات | الوظيفة |
|--------|-----------|---------|
| `CircuitBreaker` | فتح عند 50% فشل، 120 ثانية انتظار، 10 طلبات نافذة | يوقف الطلبات للخدمة الفاشلة مؤقتاً |
| `Retry` | 3 محاولات، تأخير 500ms بينها (×2) | إعادة محاولة الطلبات الفاشلة |
| `Bulkhead` | 5 متزامن، 10 في قائمة الانتظار | يحد من التوازي |
| `TimeLimiter` | 10 ثوانٍ مهلة | يلغي الطلبات التي تتجاوز المهلة |

**الاستخدام المقترح**:
```java
@CircuitBreaker(name = "fastapi", fallbackMethod = "fallbackScore")
@Retry(name = "fastapi")
@TimeLimiter(name = "fastapi")
@Bulkhead(name = "fastapi")
public CompletableFuture<Double> scoreDonor(DonorDonationData data) {
    return CompletableFuture.completedFuture(fastApiClient.score(data));
}
```

---

### 18.7 Micrometer + Prometheus - مراقبة الأداء

**الغرض**: تتبع أداء النظام واكتشاف المشاكل قبل أن تؤثر على المستخدمين.

**BloodBridgeMetrics** - المقاييس المخصصة:

| المقياس | النوع | الوظيفة |
|---------|-------|---------|
| `bloodbridge.broadcast.total` | Counter | عدد عمليات البث |
| `bloodbridge.broadcast.active` | Gauge | طلبات البث النشطة |
| `bloodbridge.donor.accepted` | Counter | عدد قبول المتبرعين |
| `bloodbridge.donor.rejected` | Counter | عدد رفض المتبرعين |
| `bloodbridge.notification.sent` | Counter | عدد الإشعارات المرسلة |
| `bloodbridge.outbox.processed` | Counter | أحداث Outbox المعالجة |
| `bloodbridge.auth.login` | Counter | عدد محاولات تسجيل الدخول |
| `bloodbridge.auth.failed` | Counter | محاولات الدخول الفاشلة |
| `bloodbridge.broadcast.duration` | Timer | زمن عملية البث |
| `bloodbridge.notification.duration` | Timer | زمن إرسال الإشعارات |
| `bloodbridge.ai.response.time` | Timer | زمن استجابة AI |
| `bloodbridge.db.query.time` | Timer | زمن استعلامات قاعدة البيانات |

**نقاط النهاية للمراقبة**:
| المسار | الوظيفة |
|--------|---------|
| `/actuator/metrics` | عرض جميع المقاييس |
| `/actuator/prometheus` | عرض بتنسيق Prometheus (لـ Grafana) |
| `/actuator/health` | فحص صحة النظام |

---

### 18.8 Problem Details (RFC 9457) - توحيد الأخطاء

**الغرض**: إرجاع أخطاء API بصيغة موحدة وقابلة للقراءة آلياً.

**هيكل الاستجابة**:
```json
{
    "type": "https://bloodbridge.app/errors/validation-error",
    "title": "Validation Error",
    "status": 400,
    "detail": "حقل البريد الإلكتروني مطلوب",
    "instance": "/api/v1/donor/blood-requests",
    "timestamp": "2026-07-25T12:00:00Z",
    "errors": {
        "email": "must not be blank"
    }
}
```

**أنواع الأخطاء المدعومة**: Validation, Authentication, Authorization, Resource Not Found, Resource Conflict, Rate Limit, Internal Server.

---

### 18.9 القفل التفاؤلي والتشاؤمي (Optimistic & Pessimistic Locking)

**الغرض**: منع تعارض البيانات في العمليات المتزامنة.

**القفل التفاؤلي** (`@Version`):

| الكيان | الحقل |
|--------|-------|
| `BloodRequest` | `@Version private Long version;` |
| `RequestResponse` | `@Version private Long version;` |
| `Donor` | `@Version private Long version;` |

القفل التفاؤلي يمنع تحديثين متزامنين لنفس السجل - إذا حدث تعارض، يتم طرح `OptimisticLockException` ويجب إعادة المحاولة.

**القفل التشاؤمي**:

```java
// في Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
Optional<BloodRequest> findByIdWithPessimisticLock(@Param("id") Long id);
```

يستخدم في `BloodRequestActionService.accept()` و `.complete()` لتأمين السجل أثناء العملية ومنع أي عملية أخرى من تعديله.

**مثال الاستخدام**:
```java
public void accept(Long responseId, User user) {
    RequestResponse response = requestResponseRepository
        .findByIdWithPessimisticLock(responseId)
        .orElseThrow(() -> new ResourceNotFoundException("Response not found"));
    
    BloodRequest request = bloodRequestRepository
        .findByIdWithPessimisticLock(response.getBloodRequest().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    
    // تنفيذ العملية...
}
```

---

### 18.10 JPA Specifications - الاستعلام الديناميكي

**الغرض**: بناء استعلامات معقدة بدون كتابة SQL يدويًا.

**المكونات**:

| المكون | الوظيفة |
|--------|---------|
| `BloodRequestSpecification` | تصفية طلبات الدم حسب: organizationId, bloodType, status, urgencyLevel, dateRange, activeOnly |
| `DonorSpecification` | تصفية المتبرعين حسب: governorate, bloodType, gender, ageRange, isEligible |

**مثال استخدام**:
```java
Specification<BloodRequest> spec = Specification
    .where(BloodRequestSpecification.byOrganization(orgId))
    .and(BloodRequestSpecification.activeRequests())
    .and(BloodRequestSpecification.withFilters(null, BloodType.A_POSITIVE, null, null, null, null, true));

List<BloodRequest> requests = bloodRequestRepository.findAll(spec, pageable);
```

---

### 18.11 Flyway V3 - ترحيل الجداول الجديدة

**الملف**: `V3__Add_infrastructure_tables.sql`

**الجداول المضافة**:

| الجدول | العمود الأساسي | الوصف |
|--------|----------------|-------|
| `outbox_events` | `id VARCHAR(36) PK` | أحداث النظام المعلقة |
| `audit_logs` | `id BIGINT AUTO_INCREMENT PK` | سجل تدقيق العمليات |
| `idempotency_keys` | `idempotency_key VARCHAR(64) PK` | مفاتيح منع التكرار |

**الأعمدة المضافة إلى الجداول الموجودة**:
- `blood_requests.version` BIGINT DEFAULT 0 (للقفل التفاؤلي)
- `request_responses.version` BIGINT DEFAULT 0 (للقفل التفاؤلي)
- `donors.version` BIGINT DEFAULT 0 (للقفل التفاؤلي)

---

### 18.12 Docker Compose المطور

**الخدمات** (6 خدمات):

| الخدمة | الصورة | الوظيفة |
|--------|--------|---------|
| `mysql` | `mysql:8.0` | قاعدة البيانات (مع volume للبيانات) |
| `redis` | `redis:7-alpine` | تخزين مؤقت + قوائم سوداء (بدون volume للمطور) |
| `prometheus` | `prom/prometheus` | جمع مقاييس الأداء |
| `grafana` | `grafana/grafana` | لوحات عرض لتحليل الأداء |
| `app` | مُبنى من Dockerfile | تطبيق Spring Boot |
| `fastapi` | صورة خارجية | خدمة AI للتقييم |

**لوحة تحكم Grafana** مهيأة مسبقاً مع:
- مصدر بيانات Prometheus
- لوحة عرض للمقاييس الرئيسية

**تشغيل التطوير**:
```bash
docker-compose up -d mysql redis    # فقط للتطوير المحلي
```

---

### 18.13 CI/CD - GitHub Actions

**الملف**: `.github/workflows/ci-cd.yml`

**الوظائف (Jobs)**:

| الوظيفة | الخطوات | الشروط |
|---------|---------|--------|
| `build-test` | Compile + Test + JaCoCo | كل push و pull request |
| `security-scan` | فحص الحزم الأمنية | بعد نجاح build-test |
| `docker-build` | بناء ونشر Docker | فقط عند push على main |

**الميزات**:
- اختبارات مع H2 (بدون MySQL أو Redis)
- تقرير JaCoCo
- بناء Docker ورفع إلى GitHub Container Registry (GHCR)

---

### 18.14 المهام المجدولة الجديدة

| المهمة | الجدول | الوصف |
|--------|--------|-------|
| `OutboxProcessor.processPendingEvents()` | كل 30 ثانية | معالجة أحداث Outbox المعلقة |
| `IdempotencyCleanup.cleanup()` | يومياً 4:00 صباحاً | حذف مفاتيح التكرار المنتهية |

---

### 18.15 إعدادات الاختبارات

**ملف**: `src/test/resources/application-test.yml`

- قاعدة بيانات **H2 في الذاكرة** مع وضع MySQL (`MODE=MYSQL`)
- تعطيل Flyway (Hibernate `ddl-auto: create-drop`)
- تعطيل Redis (`bloodbridge.redis.enabled: false`)
- تعطيل ML Scoring (`bloodbridge.scoring.ml-enabled: false`)
- FastAPI متصل بمنفذ غير صالح (`localhost:1`) مع مهلة 100ms

**خريطة الاختبارات**:

| الفئة | العدد | النوع |
|-------|-------|-------|
| `AuthServiceTest` | 7 | وحدة |
| `BloodRequestActionServiceTest` | 11 | تكامل |
| `BloodRequestBroadcastServiceTest` | 7 | تكامل |
| `DonorEligibilityServiceTest` | 8 | تكامل |
| `NotificationServiceTest` | 8 | تكامل |
| `QRCodeServiceTest` | 9 | تكامل |
| `RateLimitServiceTest` | 5 | وحدة |
| `DonorScoringServiceTest` | 5 | وحدة |
| `AuditLogServiceTest` | 4 | وحدة |
| `BloodRequestEventDrivenTest` | 5 | وحدة |
| `BloodBridgeMetricsTest` | 6 | وحدة |
| `ProblemDetailsTest` | 4 | وحدة |
| `BloodRequestSpecificationTest` | 4 | وحدة |
| `RedisRateLimiterTest` | 2 | وحدة |
| `IdempotencyFilterTest` | 3 | وحدة |
| `OutboxServiceTest` | 6 | وحدة |
| `AuthControllerSecurityTest` | 5 | تكامل |
| **المجموع** | **99** | |

---

### 18.16 JaCoCo - تغطية الكود

مضمن في مرحلة `verify`:
- **تغطية الأسطر**: ≥ 30%
- **تغطية الفروع**: ≥ 20%

التقرير يُنشأ في `target/site/jacoco/index.html`.

---

### 18.17 README شامل

تم إنشاء `README.md` متكامل يحتوي على:
- **Diagram C4**: Context و Container
- **ERD**: مخطط علاقات قاعدة البيانات
- **Sequence Diagram**: سيناريو كامل لطلب الدم
- **API Reference**: توثيق كامل لجميع endpoints
- **قرارات معمارية**: شرح لكل نمط مستخدم
- **دليل التشغيل**: تعليمات التثبيت والتشغيل المحلي

---

## 19. ملخص التحسينات (ما تمت إضافته)

| الرقم | التحسين | الوصف | الملفات |
|-------|---------|-------|---------|
| 1 | **الهيكل المعياري** | إعادة تنظيم الكود إلى وحدات (Modular Monolith) | `shared/`, `auth/`, `donor/`, إلخ |
| 2 | **الأحداث (Domain Events)** | أحداث المجال + ناشر الأحداث غير المتزامن | `DomainEventPublisher`, `AsynchronousSpringEventsConfig` |
| 3 | **Outbox Pattern** | حفظ الأحداث في جدول لضمان التسليم | `OutboxEvent`, `OutboxService`, `OutboxProcessor` |
| 4 | **سجل التدقيق** | تسجيل العمليات الهامة للرقابة | `AuditLog`, `AuditLogService` |
| 5 | **منع التكرار** | منع تنفيذ الطلب أكثر من مرة | `IdempotencyKey`, `IdempotencyFilter`, `IdempotencyCleanup` |
| 6 | **Redis** | تخزين مؤقت + نافذة منزلقة + قائمة سوداء | `RedisConfig`, `RedisRateLimiter`, `JwtBlacklistFilter` |
| 7 | **Resilience4j** | Circuit Breaker + Retry + Bulkhead + TimeLimiter | `Resilience4jConfig` |
| 8 | **Micrometer** | مقاييس أداء مخصصة (9 عدادات + 4 مؤقتات) | `BloodBridgeMetrics` |
| 9 | **Problem Details RFC 9457** | توحيد صيغة أخطاء API | `ProblemDetails`, `GlobalExceptionHandler` |
| 10 | **القفل التفاؤلي** | @Version على 3 كيانات رئيسية | `BloodRequest`, `RequestResponse`, `Donor` |
| 11 | **القفل التشاؤمي** | PESSIMISTIC_WRITE لمنافذ الخدمات | في repositories |
| 12 | **JPA Specifications** | استعلامات ديناميكية | `BloodRequestSpecification`, `DonorSpecification` |
| 13 | **Flyway V3** | جداول Outbox, Audit, Idempotency | `V3__Add_infrastructure_tables.sql` |
| 14 | **Docker Compose** | إضافة Redis, Prometheus, Grafana | `docker-compose.yml` |
| 15 | **CI/CD** | GitHub Actions مع 3 Jobs | `.github/workflows/ci-cd.yml` |
| 16 | **README** | توثيق شامل مع C4, ERD, Sequence, API | `README.md` |
| 17 | **الإعدادات** | تحديث application.yml بكل الإعدادات الجديدة | `application.yml` |
| 18 | **الأمان** | @EnableMethodSecurity و @PreAuthorize و JWT blacklist | `SecurityConfig`, جميع controllers |
| 19 | **JaCoCo** | حد أدنى لتغطية الكود (30% سطر، 20% فرع) | `pom.xml` |
| 20 | **اختبارات** | 99 اختبار (وحدة + تكامل)، H2 في الذاكرة | جميع ملفات الاختبارات |

---

> **ملاحظة**: جميع هذه التحسينات مبنية بأنماط قياسية (Design Patterns) ومكتبات معروفة لضمان قابلية التوسع والصيانة، مع تعطيل تلقائي للمكونات غير المتوفرة (مثل Redis) لتجنب فشل التشغيل في البيئات غير المكتملة.
