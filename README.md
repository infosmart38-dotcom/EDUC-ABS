# إدارة حضور التلاميذ — مشروع أندرويد (كل الملفات في مجلد واحد)

كل الملفات هنا في مستوى واحد؛ السكربت `organize.py` يرتّبها تلقائيًا في هيكل أندرويد القياسي قبل البناء.

## الحصول على APK عبر GitHub (دون تثبيت أي برنامج)
1. أنشئ مستودعًا جديدًا، ثم **Add file ▸ Upload files** وارفع كل ملفات هذا المجلد (ما عدا `android.yml`).
2. **Add file ▸ Create new file**، اكتب في خانة الاسم بالضبط: `.github/workflows/android.yml` ثم الصق داخله محتوى الملف `android.yml` واحفظ (Commit).
3. افتح **Actions ▸ Build APK ▸ Run workflow** (أو يعمل تلقائيًا عند الحفظ).
4. بعد دقائق نزّل `attendance-debug-apk` من نتيجة التشغيل وفيه `app-debug.apk`، وثبّته على الهاتف.

## البناء على حاسوب (JDK 17 + Android SDK + Gradle 8.9)
```
python3 organize.py
gradle assembleDebug
```
الناتج: `app/build/outputs/apk/debug/app-debug.apk`

## تحديث التطبيق
استبدل `index.html` بالنسخة الجديدة وأضف في أول `<head>` محتوى `shim.js` داخل وسم `<script>` (يربط التنزيل والطباعة بالنظام).
