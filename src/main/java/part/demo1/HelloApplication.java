package part.demo1;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class HelloApplication extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    // 📂 [제안 주신 최적의 경로 패치 완비] ~/Documents/TheProject/Saved Calendars/ 하위 폴더 내부 세팅
    private static final String STORAGE_DIR_PATH = System.getProperty("user.home") + File.separator
            + "Documents" + File.separator + "TheProject" + File.separator + "Saved Calendars";
    private static final String SAVE_FILE_PATH = STORAGE_DIR_PATH + File.separator + "the_project_storage.dat";

    private Map<String, ProjectData> projectStorage = new HashMap<>();
    private YearMonth currentYearMonth = YearMonth.now();

    // [데이터 저장소] 프로젝트 이름을 관리하는 클래스
    public static class ProjectData implements Serializable {
        private static final long serialVersionUID = 500L;
        String projectName;
        Map<String, TimeRecord> dateRecords = new HashMap<>();

        public ProjectData(String projectName) {
            this.projectName = projectName;
        }
    }

    // [시간 데이터 구조] 사용자가 저장한 3가지 형태의 데이터를 담는 바구니
    public static class TimeRecord implements Serializable {
        private static final long serialVersionUID = 500L;
        String type;        // "FROM_TO" or "TOTAL" or "CHECKED"
        String fromTime = "";
        String toTime = "";
        String totalTime = "";
        boolean isChecked = false;
    }

    @Override
    public void start(Stage loadingStage) {
        // 폴더 및 디렉토리가 하드디스크에 없다면 자동으로 생성해 주는 초기화 함수 가동
        initializeStorageDirectory();

        // 프로그램이 켜질 때 예전 저장 데이터를 하드디스크에서 가져옵니다.
        loadDataFromDisk();

        loadingStage.initStyle(StageStyle.TRANSPARENT);
        boolean isDarkMode = checkOSDarkMode();
        String backgroundColor = isDarkMode ? "#1E1E1E" : "#FFFFFF";
        String indicatorColor = isDarkMode ? "white" : "black";

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setStyle("-fx-progress-color: " + indicatorColor + ";");

        StackPane root = new StackPane(progressIndicator);
        root.setPrefSize(250, 250);
        root.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 25px; -fx-border-radius: 25px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 0);");

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            loadingStage.setX(event.getScreenX() - xOffset);
            loadingStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root, 250, 250);
        scene.setFill(Color.TRANSPARENT);
        loadingStage.setScene(scene);
        loadingStage.show();

        int randomTimeout = new Random().nextInt(1001) + 3000;
        PauseTransition delay = new PauseTransition(Duration.millis(randomTimeout));
        delay.setOnFinished(e -> {
            loadingStage.close();
            showMainWindow(isDarkMode);
        });
        delay.play();
    }

    // 폴더 부재 시 강제 생성 로직 추가
    private void initializeStorageDirectory() {
        try {
            File dir = new File(STORAGE_DIR_PATH);
            if (!dir.exists()) {
                dir.mkdirs(); // 계층형 하위 폴더 트리 통째로 강제 빌드
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 내 컴퓨터가 다크모드인지 감지하는 로직
    private boolean checkOSDarkMode() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                Process process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) return true;
                }
            } else if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec(new String[]{"reg", "query", "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("AppsUseLightTheme") && line.contains("0x0")) return true;
                    }
                }
            }
        } catch (Exception e) { return false; }
        return false;
    }

    // 메인 홈 화면 (New File / Continue File 선택창)
    private void showMainWindow(boolean isDarkMode) {
        try {
            Stage mainStage = new Stage();
            mainStage.initStyle(StageStyle.TRANSPARENT);

            BorderPane wrapperLayout = new BorderPane();
            String mainBgColor = isDarkMode ? "#2D2D2D" : "#FFFFFF";
            String btnColor = isDarkMode ? "#FFFFFF" : "#333333";
            String textColor = isDarkMode ? "#FFFFFF" : "#111111";

            String closeBtnHoverBg = isDarkMode ? "rgba(255, 255, 255, 0.15)" : "rgba(0, 0, 0, 0.08)";
            String closeBtnHoverText = isDarkMode ? "#FF5252" : "#C62828";

            wrapperLayout.setStyle("-fx-background-color: " + mainBgColor + "; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);");

            Button closeButton = new Button("✕");
            closeButton.setPrefSize(32, 32);
            String closeBtnStyle = "-fx-background-color: transparent; -fx-text-fill: " + btnColor + "; -fx-font-size: 15px; -fx-font-weight: 900; -fx-cursor: hand; -fx-background-radius: 6px;";
            closeButton.setStyle(closeBtnStyle);
            closeButton.setOnMouseEntered(e -> closeButton.setStyle(closeBtnStyle + "-fx-background-color: " + closeBtnHoverBg + "; -fx-text-fill: " + closeBtnHoverText + ";"));
            closeButton.setOnMouseExited(e -> closeButton.setStyle(closeBtnStyle));
            closeButton.setOnAction(e -> mainStage.close());

            StackPane customHeader = new StackPane(closeButton);
            StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
            customHeader.setPadding(new Insets(10, 10, 0, 10));

            customHeader.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            customHeader.setOnMouseDragged(event -> {
                mainStage.setX(event.getScreenX() - xOffset);
                mainStage.setY(event.getScreenY() - yOffset);
            });

            javafx.scene.text.Text topCenterText = new javafx.scene.text.Text("The Project");
            topCenterText.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-fill: " + textColor + ";");
            VBox textContainer = new VBox(topCenterText);
            textContainer.setAlignment(Pos.TOP_CENTER);
            textContainer.setPadding(new Insets(40, 0, 0, 0));

            Button leftOptionBtn = new Button("New File");
            Button rightOptionBtn = new Button("Continue File");

            leftOptionBtn.setPrefSize(140, 40);
            rightOptionBtn.setPrefSize(140, 40);

            String optBtnBg = isDarkMode ? "#444444" : "#F0F0F0";
            String optBtnHoverBg = isDarkMode ? "#555555" : "#E0E0E0";
            String optBtnStyle = "-fx-background-color: " + optBtnBg + "; -fx-text-fill: " + textColor + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8px;";

            leftOptionBtn.setStyle(optBtnStyle);
            rightOptionBtn.setStyle(optBtnStyle);

            leftOptionBtn.setOnMouseEntered(e -> leftOptionBtn.setStyle(optBtnStyle + "-fx-background-color: " + optBtnHoverBg + ";"));
            leftOptionBtn.setOnMouseExited(e -> leftOptionBtn.setStyle(optBtnStyle));
            rightOptionBtn.setOnMouseEntered(e -> rightOptionBtn.setStyle(optBtnStyle + "-fx-background-color: " + optBtnHoverBg + ";"));
            rightOptionBtn.setOnMouseExited(e -> rightOptionBtn.setStyle(optBtnStyle));

            leftOptionBtn.setOnAction(e -> handleNewCalendarWindow(mainStage, isDarkMode));
            rightOptionBtn.setOnAction(e -> handleContinueFileWindow(mainStage, isDarkMode));

            HBox bottomButtonContainer = new HBox(40, leftOptionBtn, rightOptionBtn);
            bottomButtonContainer.setAlignment(Pos.CENTER);
            bottomButtonContainer.setPadding(new Insets(0, 0, 40, 0));

            wrapperLayout.setTop(customHeader);
            wrapperLayout.setCenter(textContainer);
            wrapperLayout.setBottom(bottomButtonContainer);

            Scene scene = new Scene(wrapperLayout, 600, 400);
            scene.setFill(Color.TRANSPARENT);
            mainStage.setScene(scene);
            mainStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleNewCalendarWindow(Stage ownerStage, boolean isDarkMode) {
        TextInputDialog dialog = new TextInputDialog("UntitledCalendar");
        dialog.setTitle("New Calendar File");
        dialog.setHeaderText("Create a new work session name:");
        dialog.setContentText("Name:");
        dialog.initOwner(ownerStage);

        // [정정 완료] Optional에 <String> 타입을 확실하게 명시하여 컴파일러 오류를 파쇄했습니다.
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String projectName = result.get().trim();
            ProjectData newData = new ProjectData(projectName);
            projectStorage.put(projectName, newData);
            saveDataToDisk();
            openCalendarWindow(ownerStage, isDarkMode, newData);
        }
    }
    private void handleContinueFileWindow(Stage ownerStage, boolean isDarkMode) {
        Stage selectionStage = new Stage();
        selectionStage.initStyle(StageStyle.TRANSPARENT);
        selectionStage.initOwner(ownerStage);
        BorderPane layout = new BorderPane();
        String bg = isDarkMode ? "#222222" : "#F5F5F5";
        String txtColor = isDarkMode ? "white" : "black";
        layout.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        Label titleLabel = new Label("Select File to Continue");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + txtColor + ";");
        titleLabel.setPadding(new Insets(15, 0, 10, 15));

        // [형변환 패치 완료] 명시적으로 제네릭 타입을 정의
        ListView<String> projectListView = new ListView<>();

        // ⭐ 원본 그대로 순수한 프로젝트 이름 키셋만 담아줍니다 (이렇게 해야 파일 매칭 버그가 안 납니다!)
        projectListView.getItems().addAll(projectStorage.keySet());

        // 🎨 [UI 렌더링 및 은은한 경계선 패치]
        // 내부 데이터(String)는 오직 프로젝트 이름만 갖고 있으되, 화면에 그려질 때만 임시로 계산해서 예쁜 색과 구분선을 그려줍니다.
        projectListView.setCellFactory(lv -> new ListCell<String>() {
            private final Label mainPart = new Label();
            private final Label slashPart = new Label(" / ");
            private final Label daysPart = new Label();
            private final HBox rowContainer = new HBox(mainPart, slashPart, daysPart);

            // 완벽하다고 말씀해 주신 은은한 경계선 패널 객체
            private final Region separatorLine = new Region();
            private final VBox totalCellLayout = new VBox(rowContainer, separatorLine);

            {
                rowContainer.setAlignment(Pos.CENTER_LEFT);
                rowContainer.setSpacing(2);
                rowContainer.setPadding(new Insets(8, 12, 8, 12));

                // 경계선 두께 및 테마별 색상 고정
                separatorLine.setPrefHeight(1);
                separatorLine.setMaxWidth(Double.MAX_VALUE);
                separatorLine.setStyle("-fx-background-color: " + (isDarkMode ? "#383838" : "#E0E0E0") + ";");
                VBox.setMargin(separatorLine, new Insets(2, 12, 0, 12));
            }

            @Override
            protected void updateItem(String pName, boolean empty) {
                super.updateItem(pName, empty);

                if (empty || pName == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null); // 원본 기본 문자열 노출 차단

                    // 💡 프로젝트 이름을 토대로 맵에서 실시간 시간 통계를 계산해 냅니다.
                    ProjectData pData = projectStorage.get(pName);
                    int totalMins = 0;
                    if (pData != null && pData.dateRecords != null) {
                        for (TimeRecord rec : pData.dateRecords.values()) {
                            if ("CHECKED".equals(rec.type)) {
                                totalMins += (8 * 60);
                            } else if ("FROM_TO".equals(rec.type)) {
                                int start = parseTimeStringToMinutes(rec.fromTime);
                                int end = parseTimeStringToMinutes(rec.toTime);
                                if (end >= start) totalMins += (end - start);
                            } else if ("TOTAL".equals(rec.type)) {
                                totalMins += parseDurationStringToMinutes(rec.totalTime);
                            }
                        }
                    }

                    int hours = totalMins / 60;
                    int mins = totalMins % 60;
                    int days = hours / 8; // 8시간 기준 1일

                    // 텍스트 내용 실시간 세팅
                    mainPart.setText(String.format("%s — %d Hrs %02d Mins", pName, hours, mins));
                    daysPart.setText(String.format("%d Days", days));

                    mainPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    slashPart.setFont(Font.font("System", FontWeight.NORMAL, 13));
                    daysPart.setFont(Font.font("System", FontWeight.BOLD, 13));

                    // 클릭 선택(isSelected) 상태에 맞춰 색상 세트 적용 및 인라인 스타일 강제 고정 (깜빡임 완전 방지)
                    if (isSelected()) {
                        separatorLine.setVisible(false); // 선택 항목 바로 밑의 라인은 숨김 처리
                        if (isDarkMode) {
                            rowContainer.setStyle("-fx-background-color: #1A1A1A; -fx-background-radius: 4px; -fx-border-color: #FF9100; -fx-border-width: 1px; -fx-border-radius: 4px;");
                            mainPart.setStyle("-fx-text-fill: #E0E0E0;");
                            slashPart.setStyle("-fx-text-fill: #666666;");
                            daysPart.setStyle("-fx-text-fill: #B0BEC5;");
                        } else {
                            rowContainer.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 6, 0, 0, 2);");
                            mainPart.setStyle("-fx-text-fill: #222222;");
                            slashPart.setStyle("-fx-text-fill: #888888;");
                            daysPart.setStyle("-fx-text-fill: #000000;");
                        }
                    } else {
                        separatorLine.setVisible(true); // 평소엔 경계선 노출
                        rowContainer.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-effect: null;");
                        mainPart.setStyle("-fx-text-fill: #FF9100;"); // 기본 오렌지색 시간 정보
                        slashPart.setStyle("-fx-text-fill: #888888;"); // 슬래시 회색
                        daysPart.setStyle("-fx-text-fill: #00E5FF;");  // 일수(Days): 민트 하늘색
                    }

                    setStyle("-fx-background-color: transparent !important; -fx-padding: 0px !important;");
                    setGraphic(totalCellLayout);
                }
            }
        });

        projectListView.setStyle("-fx-background-color: " + (isDarkMode ? "#333333" : "#FFFFFF") + ";");
        if(projectListView.getItems().isEmpty()) {
            projectListView.setPlaceholder(new Label("No projects found."));
        }

        Button openBtn = new Button("Open Calendar");
        openBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        openBtn.setDisable(true);
        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) openBtn.setDisable(false);
        });

        // 🛠️ [원본 로직 100% 보존] 리스트뷰에서 꺼내오는 값은 순수 이름(String)이므로 맵 매칭이 무조건 성공합니다!
        openBtn.setOnAction(e -> {
            String selectedName = projectListView.getSelectionModel().getSelectedItem();
            if (selectedName != null) {
                selectionStage.close();
                ProjectData data = projectStorage.get(selectedName); // 👈 완벽 매칭 성공!
                openCalendarWindow(ownerStage, isDarkMode, data);    // 👈 캘린더 창 완벽 오픈!
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> selectionStage.close());
        Button locationBtn = new Button("File Location");
        locationBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // [파일 탐색기 타깃 버그 해결 원본 유지]
        locationBtn.setOnAction(e -> {
            try {
                File folder = new File(STORAGE_DIR_PATH);
                if (folder.exists()) {
                    String folderUri = folder.getCanonicalFile().toURI().toString();
                    getHostServices().showDocument(folderUri);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox footer = new HBox(12, cancelBtn, locationBtn, openBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15));
        layout.setTop(titleLabel);
        layout.setCenter(projectListView);
        layout.setBottom(footer);
        BorderPane.setMargin(projectListView, new Insets(0, 15, 0, 15));
        Scene scene = new Scene(layout, 420, 400);
        scene.setFill(Color.TRANSPARENT);
        selectionStage.setScene(scene);
        selectionStage.show();
    }

    // 📅 메인 달력 메커니즘 화면
    private void openCalendarWindow(Stage ownerStage, boolean isDarkMode, ProjectData projectData) {
        Stage calStage = new Stage();
        calStage.initStyle(StageStyle.TRANSPARENT);
        calStage.initOwner(ownerStage);
        BorderPane mainLayout = new BorderPane();
        String panelBg = isDarkMode ? "#252526" : "#FAFAFA";
        String txtColor = isDarkMode ? "#FFFFFF" : "#111111";
        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-control-inner-background: #3E3E3F; -fx-text-fill: white; -fx-prompt-text-fill: white; -fx-mark-color: white; -fx-text-base-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-control-inner-background: #FFFFFF; -fx-text-fill: black; -fx-prompt-text-fill: black; -fx-mark-color: black; -fx-text-base-color: black;";
        mainLayout.setStyle("-fx-background-color: " + panelBg + "; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 12, 0, 0, 0);");
        Label headerTitle = new Label("Working Calendar — [" + projectData.projectName + "]");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: " + txtColor + ";");
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> calStage.close());
        BorderPane topHeaderBar = new BorderPane();
        topHeaderBar.setLeft(headerTitle);
        topHeaderBar.setRight(closeBtn);
        topHeaderBar.setPadding(new Insets(12, 15, 10, 15));
        topHeaderBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        topHeaderBar.setOnMouseDragged(e -> {
            calStage.setX(e.getScreenX() - xOffset);
            calStage.setY(e.getScreenY() - yOffset);
        });
        ComboBox yearChooser = new ComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearChooser.getItems().add(y);
        yearChooser.setValue(currentYearMonth.getYear());
        yearChooser.setStyle(comboStyle);
        ComboBox monthChooser = new ComboBox<>();
        monthChooser.getItems().addAll("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
        monthChooser.getSelectionModel().select(currentYearMonth.getMonthValue() - 1);
        monthChooser.setStyle(comboStyle);
        yearChooser.setConverter(new javafx.util.converter.IntegerStringConverter());
        monthChooser.setConverter(new javafx.util.converter.DefaultStringConverter());
        GridPane calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);
        CheckBox cbToggleMode = new CheckBox("Time / Days");
        cbToggleMode.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold; -fx-cursor: hand;");
        Label lblDataValue = new Label("0 Hours");
        lblDataValue.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");
// 대시보드 통계 합산기
        Runnable calculateDashboardStats = () -> {
            int totalMinutes = 0;
            int totalDays = 0;
            for (int d = 1; d <= currentYearMonth.lengthOfMonth(); d++) {
                String dKey = currentYearMonth.atDay(d).toString();
                if (projectData.dateRecords.containsKey(dKey)) {
                    TimeRecord rec = projectData.dateRecords.get(dKey);
                    if (rec != null) {
                        totalDays++;
                        if ("CHECKED".equals(rec.type)) {
                            totalMinutes += (8 * 60);
                        } else if ("FROM_TO".equals(rec.type)) {
                            int start = parseTimeStringToMinutes(rec.fromTime);
                            int end = parseTimeStringToMinutes(rec.toTime);
                            if (end >= start) totalMinutes += (end - start);
                        } else if ("TOTAL".equals(rec.type)) {
                            totalMinutes += parseDurationStringToMinutes(rec.totalTime);
                        }
                    }
                }
            }
            if (cbToggleMode.isSelected()) {
                lblDataValue.setText(totalDays + " Days logged");
            } else {
                int displayHours = totalMinutes / 60;
                int displayMins = totalMinutes % 60;
                lblDataValue.setText(String.format("%d Hrs %02d Mins", displayHours, displayMins));
            }
        };
        cbToggleMode.setOnAction(e -> calculateDashboardStats.run());
        Runnable refreshCalendar = () -> {
            calendarGrid.getChildren().clear();
            Object selectedYearObj = yearChooser.getValue();
            int selectedYear = currentYearMonth.getYear();
            if (selectedYearObj instanceof Integer) {
                selectedYear = (Integer) selectedYearObj;
            }
            int selectedMonthIdx = monthChooser.getSelectionModel().getSelectedIndex() + 1;
            currentYearMonth = YearMonth.of(selectedYear, selectedMonthIdx);
            String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (int i = 0; i < 7; i++) {
                Label dayLabel = new Label(dayNames[i]);
                dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                dayLabel.setTextFill(isDarkMode ? Color.LIGHTGRAY : Color.DARKGRAY);
                calendarGrid.add(dayLabel, i, 0);
            }
            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue() % 7;
            int daysInMonth = currentYearMonth.lengthOfMonth();
            int row = 1;
            int col = dayOfWeekValue;
            for (int day = 1; day <= daysInMonth; day++) {
                final int currentDay = day;
                String dateKey = currentYearMonth.atDay(currentDay).toString();
                Button dayButton = new Button(String.valueOf(day));
                dayButton.setPrefSize(45, 45);
                if (projectData.dateRecords.containsKey(dateKey)) {
                    dayButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
                } else {
                    dayButton.setStyle("-fx-background-color: " + (isDarkMode ? "#3E3E3F" : "#EAEAEA") + "; -fx-text-fill: " + txtColor + "; -fx-background-radius: 6px; -fx-cursor: hand;");
                }
                dayButton.setOnAction(ev -> {
                    openTimeFeatureDialog(calStage, isDarkMode, dateKey, projectData, () -> {
                        if (projectData.dateRecords.containsKey(dateKey)) {
                            dayButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
                        } else {
                            dayButton.setStyle("-fx-background-color: " + (isDarkMode ? "#3E3E3F" : "#EAEAEA") + "; -fx-text-fill: " + txtColor + "; -fx-background-radius: 6px; -fx-cursor: hand;");
                        }
                        calculateDashboardStats.run();
                    });
                });
                calendarGrid.add(dayButton, col, row);
                col++;
                if (col > 6) { col = 0; row++; }
            }
            calculateDashboardStats.run();
        };
        yearChooser.setOnAction(e -> refreshCalendar.run());
        monthChooser.setOnAction(e -> refreshCalendar.run());
        refreshCalendar.run();
// [스크린샷 버그 차단 완비] 라벨 객체 분리 선언 후 다크모드 대응 색상 스타일 강제 부여
        Label lblYearText = new Label("Year:"); lblYearText.setStyle("-fx-text-fill: " + txtColor + ";");
        Label lblMonthText = new Label("Month:"); lblMonthText.setStyle("-fx-text-fill: " + txtColor + ";");
        HBox selectBar = new HBox(12, lblYearText, yearChooser, lblMonthText, monthChooser);
        selectBar.setAlignment(Pos.CENTER);
        selectBar.setPadding(new Insets(10, 0, 15, 0));
        VBox centerArea = new VBox(10, selectBar, calendarGrid);
        centerArea.setPadding(new Insets(10, 15, 10, 15));
        BorderPane footerDashboard = new BorderPane();
        footerDashboard.setPadding(new Insets(10, 20, 20, 20));
        footerDashboard.setLeft(cbToggleMode);
        footerDashboard.setRight(lblDataValue);
        mainLayout.setTop(topHeaderBar);
        mainLayout.setCenter(centerArea);
        mainLayout.setBottom(footerDashboard);
        Scene scene = new Scene(mainLayout, 450, 520);
        scene.setFill(Color.TRANSPARENT);
        calStage.setScene(scene);
        calStage.show();
    }
    private void openTimeFeatureDialog(Stage parentStage, boolean isDarkMode, String dateKey, ProjectData projectData, Runnable onSaveSuccess) {
        Stage dialogStage = new Stage(StageStyle.UTILITY);
        dialogStage.initOwner(parentStage);
        dialogStage.setTitle("Time Logger [" + dateKey + "]");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: " + (isDarkMode ? "#2D2D2D" : "#FFFFFF") + ";");
        Label mainPrompt = new Label("Select Prefered Time Recording Feature:");
        mainPrompt.setStyle("-fx-text-fill: " + (isDarkMode ? "white" : "black") + "; -fx-font-weight: bold;");
        ToggleGroup featureGroup = new ToggleGroup();
        RadioButton rbFromTo = new RadioButton("From Time to This Time");
        RadioButton rbTotal = new RadioButton("Total Time");
        RadioButton rbChecked = new RadioButton("Checked (8 Hours)");
        rbFromTo.setToggleGroup(featureGroup);
        rbTotal.setToggleGroup(featureGroup);
        rbChecked.setToggleGroup(featureGroup);
        String radioStyle = "-fx-text-fill: " + (isDarkMode ? "white" : "black") + ";";
        rbFromTo.setStyle(radioStyle); rbTotal.setStyle(radioStyle); rbChecked.setStyle(radioStyle);
        List amPmList = new ArrayList<>();
        String[] ampmArray = {"AM", "PM"};
        for (String ampm : ampmArray) {
            for (int h = 1; h <= 12; h++) {
                for (int m = 0; m < 60; m += 15) {
                    amPmList.add(String.format("%02d:%02d %s", h, m, ampm));
                }
            }
        }
        ComboBox cbFrom = new ComboBox<>(); cbFrom.getItems().addAll(amPmList); cbFrom.setValue("09:00 AM");
        ComboBox cbTo = new ComboBox<>(); cbTo.getItems().addAll(amPmList); cbTo.setValue("06:00 PM");
        List durationList = new ArrayList<>();
        for (int h = 0; h <= 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (h == 0 && m == 0) continue;
                durationList.add(String.format("%02d:%02d", h, m));
                if (h == 24) break;
            }
        }
        ComboBox cbTotalDuration = new ComboBox<>();
        cbTotalDuration.getItems().addAll(durationList);
        cbTotalDuration.setValue("08:00");
        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-text-fill: white; -fx-control-inner-background: #3E3E3F; -fx-text-base-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-text-fill: black; -fx-control-inner-background: #FFFFFF; -fx-text-base-color: black;";
        cbFrom.setStyle(comboStyle); cbTo.setStyle(comboStyle); cbTotalDuration.setStyle(comboStyle);
        HBox hboxFromTo = new HBox(8, new Label("From:"), cbFrom, new Label("To:"), cbTo);
        HBox hboxTotal = new HBox(8, new Label("Duration:"), cbTotalDuration);
        Label lblCheckedHint = new Label("💡 Select this option to register exactly 8 hours instantly.");
        lblCheckedHint.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px; -fx-font-style: italic;");
        hboxFromTo.disableProperty().bind(rbFromTo.selectedProperty().not());
        hboxTotal.disableProperty().bind(rbTotal.selectedProperty().not());
        lblCheckedHint.disableProperty().bind(rbChecked.selectedProperty().not());
        TimeRecord existing = projectData.dateRecords.get(dateKey);
        if (existing != null) {
            if ("FROM_TO".equals(existing.type)) {
                rbFromTo.setSelected(true);
                cbFrom.setValue(existing.fromTime); cbTo.setValue(existing.toTime);
            } else if ("TOTAL".equals(existing.type)) {
                rbTotal.setSelected(true);
                cbTotalDuration.setValue(existing.totalTime);
            } else if ("CHECKED".equals(existing.type)) {
                rbChecked.setSelected(true);
            }
        } else {
            rbFromTo.setSelected(true);
        }
        Button deleteBtn = new Button("Clear Record");
        deleteBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            projectData.dateRecords.remove(dateKey);
            saveDataToDisk(); onSaveSuccess.run(); dialogStage.close();
        });
        Button saveBtn = new Button("Save Record");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            TimeRecord record = new TimeRecord();
            if (rbFromTo.isSelected()) {
                record.type = "FROM_TO";
                record.fromTime = String.valueOf(cbFrom.getValue());
                record.toTime = String.valueOf(cbTo.getValue());
            } else if (rbTotal.isSelected()) {
                record.type = "TOTAL";
                record.totalTime = String.valueOf(cbTotalDuration.getValue());
            } else if (rbChecked.isSelected()) {
                record.type = "CHECKED";
                record.isChecked = true;
            }
            projectData.dateRecords.put(dateKey, record);
            saveDataToDisk(); onSaveSuccess.run(); dialogStage.close();
        });
        HBox btnContainer = new HBox(15, deleteBtn, saveBtn);
        btnContainer.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(mainPrompt, rbFromTo, hboxFromTo, rbTotal, hboxTotal, rbChecked, lblCheckedHint, btnContainer);
        dialogStage.setScene(new Scene(layout, 370, 420));
        dialogStage.show();
    }
    private int parseTimeStringToMinutes(String timeStr) {
        try {
            if (timeStr == null || !timeStr.contains(":") || !timeStr.contains(" ")) return 0;
            String[] spaceSplit = timeStr.split(" ");
            String rawClock = spaceSplit[0];
            String ampm = spaceSplit[1];
            String[] colonSplit = rawClock.split(":");
            int hour = Integer.parseInt(colonSplit[0]);
            int min = Integer.parseInt(colonSplit[1]);
            if ("PM".equalsIgnoreCase(ampm) && hour < 12) hour += 12;
            if ("AM".equalsIgnoreCase(ampm) && hour == 12) hour = 0;
            return (hour * 60) + min;
        } catch (Exception e) { return 0; }
    }
    private int parseDurationStringToMinutes(String durationStr) {
        try {
            if (durationStr == null || !durationStr.contains(":")) return 0;
            String[] colonSplit = durationStr.split(":");
            int hour = Integer.parseInt(colonSplit[0]);
            int min = Integer.parseInt(colonSplit[1]);
            return (hour * 60) + min;
        } catch (Exception e) { return 0; }
    }
    private void saveDataToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE_PATH))) {
            oos.writeObject(projectStorage);
        } catch (IOException e) { e.printStackTrace(); }
    }
    @SuppressWarnings("unchecked")
    private void loadDataFromDisk() {
        File dataFile = new File(SAVE_FILE_PATH);
        if (!dataFile.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Object readObject = ois.readObject();
            if (readObject instanceof Map) {
                projectStorage = (Map<String, ProjectData>) readObject;
            }
        } catch (Exception e) {
            dataFile.delete();
        }
    }
    public static void main(String[] args) { launch(); }
}