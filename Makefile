PACKAGE = de.markusfisch.android.binaryeye

all: debug install start

debug:
	./gradlew assembleDebug

release: lint
	./gradlew assembleRelease

bundle: lint
	./gradlew bundleRelease

lint:
	./gradlew lintDebug

infer: clean
	infer -- ./gradlew assembleDebug

test:
	./gradlew test

testview:
	adb shell am start -W -a android.intent.action.VIEW -d 'binaryeye://scan'

testscan:
	adb shell am start -a android.intent.action.VIEW \
		-c android.intent.category.BROWSABLE \
		-d 'binaryeye://scan/?ret=http%3A%2F%2Fmarkusfisch.de%2F%3Fresult%3D{RESULT}'

testurl:
	adb shell am start -a android.intent.action.VIEW \
		-c android.intent.category.BROWSABLE \
		-d 'http://markusfisch.de/BinaryEye?ret=http%3A%2F%2Fmarkusfisch.de%2F%3Fresult%3D{RESULT}'

testxiaomi:
	adb shell am start -a android.intent.action.VIEW \
		-c android.intent.category.BROWSABLE \
		-d 'market://details?id=com.xiaomi.scanner'

install:
	adb $(TARGET) install -r app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n \
		$(PACKAGE).debug/$(PACKAGE).activity.SplashActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE).debug

meminfo:
	adb shell dumpsys meminfo $(PACKAGE).debug

images:
	svg/update.sh

avocado:
	avocado $(shell fgrep -rl '<vector' app/src/main/res)

clean:
	./gradlew clean
