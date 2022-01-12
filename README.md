# GaugeView

Gauge views (charts) for Android.

![](https://raw.githubusercontent.com/zhjl37/GaugeView/main/screenshot.jpeg)

## Gradle

```
implementation 'com.zhjl37.gaugeview:gaugeview:1.0.0'
```

## Usage

### Create GaugeView in XML

```
<me.zhjl37.gaugeview.GradeGaugeView
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="[left|top|right|bottom|center|center_horizontal|center_vertical]"
    app:useGradient="[true|false]" />
```

### Initialize GaugeView

```
gaugeView.setLabel("BMI");
gaugeView.setAdapter(new Adapter4Test());
```

### Sets the current value

```
gaugeView.setCurrent(20f);
```
