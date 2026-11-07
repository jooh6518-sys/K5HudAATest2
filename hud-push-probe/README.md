# K5 HUD 직접 출력 시험 v1.0

용도: 국내형 2021 K5 DL3HEV (V039.001.260902) Gen5W에서 NavigationCluster API로 테스트 우회전 안내 300m를 한 번 전송해 순정 HUD에 나오는지 확인합니다.

NEMESIS 패키지로만 설치하세요. Android Auto 또는 네이버지도 후킹 기능은 이 버전에 없습니다.

- 순정 내비 길안내 종료, Android Auto 연결 해제, 기어 P 및 주차브레이크 상태에서만 실행하세요.
- 버튼 1은 API 존재 확인만 합니다.
- 버튼 2는 안전 확인과 추가 승인 후 우회전 300m를 5초 동안 전송해 보고, 지우기 함수를 호출합니다.
- 버튼 3은 사용자 지우기입니다.
- 주행 중 사용 금지. 실제 HUD 표시는 아직 검증되지 않았습니다.
- 클래스 접근 권한이 없거나 HUD 표시에 필요한 외부 상태가 없는 경우 화면에 오류가 기록됩니다.
- 서비스/시스템 라이브러리와 Wireless Projection을 수정하지 않습니다.
- 앱 패키지명 kr.k5.hudprobe, NEMESIS 패키지 ID k5.hud.probe.
- NEMESIS Manager My Packages에서 제거할 수 있습니다.

참고: 차량 진단보고서(342개 hook 지점 중 TBT 함수 발견), Chunwol/gen5w-kr V039 NavigationCluster 호출과 0x43 아이콘/미터 인자 구현. 함수 발견은 실제 출력과 다릅니다.
