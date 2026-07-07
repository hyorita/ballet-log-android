# Ballet Log 1.11 — 업로드 자료

versionCode 12 / versionName 1.11

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.11.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.11 동기화. Log 탭 월 접기.

---

## 1.11 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.

### 한국어

```
✨ 1.11
• 월 접기 — Log 탭에서 월 헤더를 탭하면 그 달을 접을 수 있어요. 접힌 달은 간단한 요약(사진 · 운동)을 보여주고, 다음에 앱을 열어도 접힌 상태가 유지돼서 긴 기록을 훑어보기 편합니다.
```

### English

```
✨ 1.11
• Collapse months — Tap a month header in the Log tab to fold it away. Collapsed months show a quick summary (photos · workouts) and stay collapsed next time you open the app, so a long history is easy to skim.
```

### 日本語

```
✨ 1.11
• 月を折りたたむ — Log タブで月のヘッダーをタップすると、その月を折りたためます。折りたたんだ月は簡単な概要（写真・ワークアウト）を表示し、次にアプリを開いても折りたたんだ状態が保たれます。
```

---

## 1.11 변경사항 (개발자 메모)

iOS 1.11은 3개 항목(월 접기 / 백업 파일 열어 복원 / export 전 WAL checkpoint 수정). Android는 그중 **월 접기만** 반영.

| iOS 항목 | Android 대응 |
|---|---|
| **월 접기** (`d0362f7`) | ✅ 포팅. `PhotoLogScreen.kt` 월 헤더를 탭 가능하게 — chevron 회전 + 접힘 시 "사진 N장 · 운동 M개" 요약. 접힌 달은 콜라주·strip 렌더 스킵. `CollapsedMonthsPreferences`(SharedPreferences, "yyyy-MM" 콤마 조인)로 영속화, 기본 전부 펼침(비파괴적). `CollageGroup.photoCount` 추가, `log_month_photos`/`log_month_workouts` 문자열(en/ko/ja). |
| **export 전 WAL checkpoint** (`606dc86`) | ✅ 이미 반영됨. Android `BackupManager.exportToUri`는 export 직전 `PRAGMA wal_checkpoint(FULL)` 수행 (1.x 초기 구현부터). 추가 작업 없음. |
| **백업 파일 열어 복원** (`1468c58`) | ⏸️ 이번엔 생략(사용자 결정). Android는 커스텀 `.bblbackup` 확장자 연결이 불안정(content:// URI는 확장자 미노출 → octet-stream/zip 광범위 매칭 필요)해 부작용 대비 가치 낮음. 복원은 설정→데이터 가져오기로 계속 가능. 향후 필요 시 범위 제한 intent-filter로 추가 검토. |

**Room 스키마 변경 없음(version 6 유지) → 백업 양방향 안전.**

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.11.apk` (versionCode 12)
3. 위 "1.11 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 12는 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시 재사용 불가 → 1.11.1 (versionCode 13)로 bump. (1.8 → 1.8.1 전례 참조)
