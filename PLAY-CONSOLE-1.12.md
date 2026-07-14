# Ballet Log 1.12 — 업로드 자료

versionCode 13 / versionName 1.12

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.12.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.12 동기화. 사진 편집 시 사진 교체 + Log 그리드 태그 필터.

---

## 1.12 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.

### 한국어

```
✨ 1.12
• 사진 바꾸기 — 이제 Photo Log를 편집할 때 캡션·태그뿐 아니라 사진 자체도 바꿀 수 있어요. 로그를 열고 위쪽 사진 버튼을 눌러 새 사진을 고르면, 캡션·태그·운동·날짜는 그대로 유지됩니다.
• 태그로 거르기 — Log 그리드 위의 태그(스튜디오·레벨·선생님)를 탭하면 원하는 것만 볼 수 있어요. 여러 개를 골라 조합할 수도 있고, 언제든 지우면 다시 전체가 보입니다.
```

### English

```
✨ 1.12
• Change a photo — Editing a Photo Log now lets you swap the picture, not just the caption and tags. Open a log, tap the photo button at the top, and pick a new one — your caption, tags, workout, and date stay put.
• Filter by tag — Tap the tags above the Log grid (studio, level, teacher) to narrow it down. Pick more than one to combine them, and clear anytime to see everything again.
```

### 日本語

```
✨ 1.12
• 写真を変更 — Photo Log を編集する際、キャプションやタグだけでなく写真そのものも変更できるようになりました。ログを開いて上部の写真ボタンをタップし、新しい写真を選ぶだけ。キャプション・タグ・ワークアウト・日付はそのまま残ります。
• タグで絞り込み — Log グリッド上部のタグ（スタジオ・レベル・講師）をタップすると絞り込めます。複数選んで組み合わせることもでき、いつでもクリアすればすべて表示に戻ります。
```

---

## 1.12 변경사항 (개발자 메모)

iOS 1.12는 2개 항목(사진 교체 / 태그 필터). Android는 **둘 다 반영**.

| iOS 항목 | Android 대응 |
|---|---|
| **사진 교체** (`9769819`) | ✅ 포팅. 편집 모드 상단에 "사진 바꾸기" 버튼(닫기 옆, `PhotoLibrary` 아이콘) 추가 → 기존 갤러리 picker 재실행. Android는 picker가 매번 새 UUID 파일로 저장하므로 iOS의 "새 파일명으로 재기록" 트릭이 자동 성립 → 캐시 stale 이슈 없음. 교체 시 `filteredPhotoPath`/`filterName`을 Original로 리셋하고, 옛 원본·필터본 파일을 `deleteOrphanPhotos(keep=새이름)`로 정리. 날짜·캡션·태그·운동은 유지(신규 로그만 사진 촬영일 채택). picker 취소 시 기존 사진 유지. `PhotoLogEditScreen`, `PhotoLogViewModel.deleteOrphanPhotos`. |
| **태그 필터** (`714def5`) | ✅ 포팅. `PhotoLogScreen` 콜라주 위에 가로 스크롤 태그 칩 행(studio/level/teacher, distinct+정렬). 다중선택 AND(`log.tags.toSet().containsAll(selectedTags)`), 앞쪽 clear(×) 칩, 빈 결과 시 "No results"(기존 `no_results` 재사용). 선택은 채워진 캡슐만으로 표시(체크마크 없음). 운동 전용 플레이스홀더는 태그가 없어 필터 시 자연 제외 → 월 그룹·접기·strip 밴드 모두 공유 `filteredLogs` 경유로 유지. 삭제된 태그 자동 pruning(`LaunchedEffect(allTags)`)으로 stale 필터 방지. `log_clear_filters` 문자열(en/ko/ja). |

**Room 스키마 변경 없음(version 6 유지) → 백업 양방향 안전.**

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.12.apk` (versionCode 13)
3. 위 "1.12 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 13은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시 재사용 불가 → 1.12.1 (versionCode 14)로 bump. (1.8 → 1.8.1 전례 참조)
