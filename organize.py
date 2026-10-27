# يرتّب الملفات المسطّحة في هيكل مشروع أندرويد القياسي (يُشغَّل مرة واحدة قبل البناء)
import os, shutil
here = os.path.dirname(os.path.abspath(__file__))
M = os.path.join(here, 'app', 'src', 'main')
MAP = {
    'MainActivity.java': 'java/dz/attendance/app/MainActivity.java',
    'AndroidManifest.xml': 'AndroidManifest.xml',
    'index.html': 'assets/index.html',
    'strings.xml': 'res/values/strings.xml',
    'styles.xml': 'res/values/styles.xml',
    'colors.xml': 'res/values/colors.xml',
    'ic_fg.xml': 'res/drawable/ic_fg.xml',
    'ic_launcher.xml': 'res/mipmap-anydpi-v26/ic_launcher.xml',
}
for src, dst in MAP.items():
    d = os.path.join(M, dst)
    os.makedirs(os.path.dirname(d), exist_ok=True)
    shutil.copy(os.path.join(here, src), d)
shutil.copy(os.path.join(here, 'app_build.gradle'), os.path.join(here, 'app', 'build.gradle')) if os.path.isdir(os.path.join(here, 'app')) else None
os.makedirs(os.path.join(here, 'app'), exist_ok=True)
shutil.copy(os.path.join(here, 'app_build.gradle'), os.path.join(here, 'app', 'build.gradle'))
shutil.copy(os.path.join(here, 'root_build.gradle'), os.path.join(here, 'build.gradle'))
print('تم ترتيب المشروع')
