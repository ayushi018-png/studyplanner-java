package com.studyplanner;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.awt.Toolkit;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StudyPlannerApp extends Application {

    private final ObservableList<StudyTask> tasks = FXCollections.observableArrayList();
    private final Path store = Paths.get(System.getProperty("user.home"), ".studyplanner", "tasks.dat");
    private BorderPane root;
    private VBox page;
    private Label pageTitle;
    private String current = "Dashboard";
    private final String bg = "#F5F6FA", ink = "#202642", muted = "#777F95", purple = "#6658D3";
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void start(Stage stage) {
        load();
        root = new BorderPane();
        root.setStyle("-fx-background-color:" + bg + ";");
        root.setLeft(sidebar());

        VBox shell = new VBox(0);
        HBox top = topbar();
        page = new VBox(20);
        page.setPadding(new Insets(26, 32, 36, 32));
        page.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        shell.getChildren().addAll(top, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setCenter(shell);

        double w = 1200, h = 820;
        try {
            var d = Toolkit.getDefaultToolkit().getScreenSize();
            w = d.width;
            h = d.height;
        } catch (Exception ignored) {}

        stage.setTitle("StudyPlanner");
        stage.setMinWidth(850);
        stage.setMinHeight(620);
        stage.setScene(new Scene(root, Math.min(1360, w * 0.9), Math.min(900, h * 0.9)));
        stage.show();
        show("Dashboard");
    }

    private VBox sidebar() {
        VBox s = new VBox(12);
        s.setPadding(new Insets(28, 16, 24, 16));
        s.setPrefWidth(225);
        s.setStyle("-fx-background-color:#222844;");

        Label logo = new Label("✦  StudyPlanner");
        logo.setTextFill(Color.WHITE);
        logo.setFont(Font.font("System", 21));

        Label sub = new Label("YOUR STUDY SPACE");
        sub.setTextFill(Color.web("#AEB4D0"));
        sub.setStyle("-fx-font-size:10px;-fx-font-weight:bold;");
        s.getChildren().addAll(logo, new Label(" "), sub);

        for (String n : List.of("Dashboard", "All Tasks", "Subjects", "Completed")) {
            Button b = new Button(navIcon(n) + "   " + n);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setTextFill(Color.WHITE);
            b.setStyle(navStyle(false));
            b.setOnAction(e -> show(n));
            s.getChildren().add(b);
        }

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);
        Label quote = new Label("Small steps every day lead to big results.");
        quote.setWrapText(true);
        quote.setTextFill(Color.web("#D8DBEB"));
        quote.setStyle("-fx-font-style:italic;-fx-font-size:12px;");
        s.getChildren().addAll(sp, quote);
        return s;
    }

    private String navIcon(String n) {
        return switch (n) {
            case "Dashboard" -> "⌂";
            case "All Tasks" -> "☷";
            case "Subjects" -> "▤";
            default -> "✓";
        };
    }

    private String navStyle(boolean active) {
        return "-fx-background-color:" + (active ? "#514A91" : "transparent")
                + ";-fx-background-radius:9;-fx-padding:13 12;-fx-font-size:13px;";
    }

    private HBox topbar() {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(17, 32, 15, 32));
        h.setStyle("-fx-background-color:white;-fx-border-color:transparent transparent #E8EAF1 transparent;");

        pageTitle = new Label("Dashboard");
        pageTitle.setFont(Font.font("System", 18));
        pageTitle.setTextFill(Color.web(ink));

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        date.setTextFill(Color.web(muted));
        h.getChildren().addAll(pageTitle, gap, date);
        return h;
    }

    private void show(String name) {
        current = name;
        pageTitle.setText(name);
        page.getChildren().clear();

        switch (name) {
            case "Dashboard" -> dashboard();
            case "All Tasks" -> taskPage(false);
            case "Completed" -> taskPage(true);
            case "Subjects" -> subjectsPage();
        }
    }

    private void heading(String title, String subtitle) {
        Label t = new Label(title);
        t.setFont(Font.font("System", 26));
        t.setTextFill(Color.web(ink));
        t.setStyle("-fx-text-fill: #202642;");

        Label st = new Label(subtitle);
        st.setTextFill(Color.web(muted));
        st.setStyle("-fx-text-fill: #777F95;");
        st.setFont(Font.font(13));
        page.getChildren().addAll(t, st);
    }

    private void dashboard() {
        heading("Good to see you!", "Here is your study progress at a glance.");

        int done = (int) tasks.stream().filter(t -> t.done).count();
        int pending = tasks.size() - done;
        double p = tasks.isEmpty() ? 0 : (double) done / tasks.size();

        FlowPane stats = new FlowPane(14, 14);
        stats.setPrefWrapLength(900);
        stats.getChildren().addAll(
                stat("TOTAL TASKS", String.valueOf(tasks.size()), "#EEEAFE"),
                stat("COMPLETED", String.valueOf(done), "#E3F6EC"),
                stat("PENDING", String.valueOf(pending), "#FFF1DE"),
                stat("PROGRESS", (int) (p * 100) + "%", "#E5F1FF")
        );
        page.getChildren().add(stats);

        VBox progress = panel();
        Label pl = new Label("Overall progress");
        pl.setFont(Font.font("System", 16));
        pl.setTextFill(Color.web(ink));
        pl.setStyle("-fx-text-fill: #202642;");

        ProgressBar bar = new ProgressBar(p);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(13);

        Label pct = new Label((int) (p * 100) + "% completed");
        pct.setTextFill(Color.web(purple));
        progress.getChildren().addAll(pl, bar, pct);
        page.getChildren().add(progress);

        HBox row = new HBox(12);
        Label upcoming = new Label("Upcoming tasks");
        upcoming.setFont(Font.font("System", 19));
        upcoming.setTextFill(Color.web(ink));
        upcoming.setStyle("-fx-text-fill: #202642;");

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Button view = new Button("View all tasks  →");
        view.setOnAction(e -> show("All Tasks"));
        styleSecondary(view);
        row.getChildren().addAll(upcoming, gap, view);
        page.getChildren().add(row);

        List<StudyTask> next = tasks.stream()
                .filter(t -> !t.done)
                .sorted(Comparator.comparing(t -> t.due))
                .limit(4).toList();

        if (next.isEmpty()) {
            page.getChildren().add(empty("No pending tasks yet. Add a task to get started."));
        } else {
            next.forEach(t -> page.getChildren().add(taskCard(t)));
        }

        Button add = primary("＋  Add a new task");
        add.setOnAction(e -> edit(null));
        page.getChildren().add(add);
    }

    private VBox stat(String label, String value, String color) {
        VBox b = panel();
        b.setPrefWidth(190);
        b.setMinHeight(105);
        b.setStyle("-fx-background-color:white;-fx-background-radius:14;-fx-border-color:#EAECF3;-fx-border-radius:14;");

        Label l = new Label(label);
        l.setTextFill(Color.web(muted));
        l.setStyle("-fx-text-fill: #777F95; -fx-font-size:10px;-fx-font-weight:bold;");

        Label v = new Label(value);
        v.setFont(Font.font("System", 27));
        v.setTextFill(Color.web(ink));
        v.setStyle("-fx-text-fill: #202642;");
        b.getChildren().addAll(l, v);
        return b;
    }

    private void taskPage(boolean completed) {
        heading(
                completed ? "Completed tasks" : "All your tasks",
                completed ? "A record of the work you have finished."
                        : "Keep every assignment, revision and deadline in one place."
        );

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField();
        search.setPromptText("Search by task or subject...");
        search.setPrefWidth(280);

        ComboBox<String> filter = new ComboBox<>(
                FXCollections.observableArrayList("All", "Pending", "Due today", "Overdue")
        );
        filter.setValue("All");
        controls.getChildren().addAll(search, filter);

        if (!completed) {
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Button add = primary("＋ Add task");
            add.setOnAction(e -> edit(null));
            controls.getChildren().addAll(gap, add);
        }

        page.getChildren().add(controls);
        VBox list = new VBox(10);
        list.setFillWidth(true);

        Runnable refresh = () -> {
            list.getChildren().clear();
            String q = search.getText().toLowerCase();
            String f = filter.getValue();

            List<StudyTask> found = tasks.stream()
                    .filter(t -> t.done == completed)
                    .filter(t -> (t.title + " " + t.subject).toLowerCase().contains(q))
                    .filter(t -> completed || switch (f) {
                        case "Pending" -> !t.done;
                        case "Due today" -> t.due.equals(LocalDate.now());
                        case "Overdue" -> t.due.isBefore(LocalDate.now());
                        default -> true;
                    })
                    .sorted(Comparator.comparing(t -> t.due))
                    .toList();

            if (found.isEmpty()) {
                list.getChildren().add(empty(completed ? "No completed tasks yet."
                        : "No tasks match this view. Add a task or change your filters."));
            } else {
                found.forEach(t -> list.getChildren().add(taskCard(t)));
            }
        };

        search.textProperty().addListener((o, a, b) -> refresh.run());
        filter.setOnAction(e -> refresh.run());
        page.getChildren().add(list);
        refresh.run();
    }

    private void subjectsPage() {
        heading("Your subjects", "See how your study tasks are organised by subject.");

        Map<String, List<StudyTask>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.subject, TreeMap::new, Collectors.toList()));

        if (grouped.isEmpty()) {
            page.getChildren().add(empty("No subjects yet. Add a task and enter its subject to see it here."));
            return;
        }

        for (var entry : grouped.entrySet()) {
            VBox card = panel();
            HBox top = new HBox();

            Label name = new Label(entry.getKey());
            name.setFont(Font.font("System", 17));
            name.setTextFill(Color.web(ink));
            name.setStyle("-fx-text-fill: #202642;");

            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);

            long done = entry.getValue().stream().filter(t -> t.done).count();
            Label count = new Label(done + " / " + entry.getValue().size() + " completed");
            count.setTextFill(Color.web(muted));
            count.setStyle("-fx-text-fill: #777F95;");

            top.getChildren().addAll(name, gap, count);

            ProgressBar bar = new ProgressBar((double) done / entry.getValue().size());
            bar.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(top, bar);

            for (StudyTask t : entry.getValue()) {
                Label item = new Label((t.done ? "✓  " : "•  ") + t.title + "   ·   " + t.due.format(fmt));
                item.setWrapText(true);
                item.setStyle("-fx-text-fill: #202642;");
                card.getChildren().add(item);
            }
            page.getChildren().add(card);
        }
    }

    private HBox taskCard(StudyTask t) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 17, 15, 17));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#E8EAF1;-fx-border-radius:12;");

        VBox info = new VBox(6);
        Label title = new Label((t.done ? "✓  " : "") + t.title);
        title.setWrapText(true);
        title.setMaxWidth(650);
        title.setFont(Font.font("System", 14));
        title.setTextFill(Color.web(t.done ? muted : ink));
        title.setStyle("-fx-text-fill: " + (t.done ? "#777F95" : "#202642") + ";");
        if (t.done) title.setStyle("-fx-strikethrough:true;");

        Label meta = new Label(t.subject + "     •     Due " + t.due.format(fmt) + "     •     " + t.priority + " priority");
        meta.setWrapText(true);
        meta.setTextFill(Color.web(muted));
        meta.setStyle("-fx-text-fill: #777F95; -fx-font-size:11px;");

        info.setMinWidth(0);
        info.getChildren().addAll(title, meta);

        if (!t.notes.isBlank()) {
            Label notes = new Label(t.notes);
            notes.setWrapText(true);
            notes.setMaxWidth(650);
            notes.setTextFill(Color.web(muted));
            notes.setStyle("-fx-text-fill: #777F95; -fx-font-size:11px;");
            info.getChildren().add(notes);
        }

        HBox.setHgrow(info, Priority.ALWAYS);

        Button toggle = new Button(t.done ? "Reopen" : "Complete");
        styleSecondary(toggle);
        toggle.setOnAction(e -> {
            t.done = !t.done;
            save();
            show(current);
        });

        Button edit = new Button("Edit");
        styleSecondary(edit);
        edit.setOnAction(e -> edit(t));

        Button del = new Button("Delete");
        del.setStyle("-fx-background-color:#FFF0F0;-fx-text-fill:#B94C4C;-fx-background-radius:8;");
        del.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this task?", ButtonType.CANCEL, ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait().filter(x -> x == ButtonType.OK).ifPresent(x -> {
                tasks.remove(t);
                save();
                show(current);
            });
        });

        VBox actions = new VBox(6, toggle, edit, del);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(104);
        card.getChildren().addAll(info, actions);
        return card;
    }

    private VBox panel() {
        VBox b = new VBox(12);
        b.setPadding(new Insets(18));
        b.setStyle("-fx-background-color:white;-fx-background-radius:14;-fx-border-color:#E8EAF1;-fx-border-radius:14;");
        return b;
    }

    private VBox empty(String text) {
        VBox b = panel();
        Label l = new Label(text);
        l.setWrapText(true);
        l.setTextFill(Color.web(muted));
        l.setStyle("-fx-text-fill: #777F95;");
        b.getChildren().add(l);
        return b;
    }

    private Button primary(String text) {
        Button b = new Button(text);
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + purple + ";-fx-background-radius:9;-fx-padding:11 16;-fx-font-weight:bold;");
        return b;
    }

    private void styleSecondary(Button b) {
        b.setStyle("-fx-background-color:#F0EFFB;-fx-text-fill:" + purple + ";-fx-background-radius:8;-fx-padding:8 11;");
    }

    private void edit(StudyTask old) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(old == null ? "Add study task" : "Edit study task");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField title = new TextField(old == null ? "" : old.title);
        TextField subject = new TextField(old == null ? "" : old.subject);
        title.setPromptText("e.g. Revise Java inheritance");
        subject.setPromptText("e.g. Java");

        DatePicker due = new DatePicker(old == null ? LocalDate.now().plusDays(1) : old.due);
        ComboBox<String> priority = new ComboBox<>(FXCollections.observableArrayList("High", "Medium", "Low"));
        priority.setValue(old == null ? "Medium" : old.priority);

        TextArea notes = new TextArea(old == null ? "" : old.notes);
        notes.setPromptText("Optional notes");
        notes.setPrefRowCount(3);

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(12);
        g.setPadding(new Insets(12));
        g.addRow(0, new Label("Task name"), title);
        g.addRow(1, new Label("Subject"), subject);
        g.addRow(2, new Label("Due date"), due);
        g.addRow(3, new Label("Priority"), priority);
        g.addRow(4, new Label("Notes"), notes);

        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(subject, Priority.ALWAYS);
        GridPane.setHgrow(notes, Priority.ALWAYS);
        g.setMinWidth(420);
        d.getDialogPane().setContent(g);

        d.showAndWait().filter(x -> x == ButtonType.OK).ifPresent(x -> {
            if (title.getText().isBlank() || subject.getText().isBlank() || due.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Please enter a task name, subject and due date.").showAndWait();
                return;
            }

            StudyTask t = old == null ? new StudyTask() : old;
            t.title = title.getText().trim();
            t.subject = subject.getText().trim();
            t.due = due.getValue();
            t.priority = priority.getValue();
            t.notes = notes.getText().trim();

            if (old == null) tasks.add(t);
            save();
            show(current);
        });
    }

    private void save() {
        try {
            Files.createDirectories(store.getParent());
            try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(store))) {
                o.writeObject(new ArrayList<>(tasks));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not save tasks: " + e.getMessage()).showAndWait();
        }
    }

    private void load() {
        if (!Files.exists(store)) return;

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(store))) {
            Object o = in.readObject();
            if (o instanceof List<?> l) {
                for (Object x : l) {
                    if (x instanceof StudyTask t) tasks.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static class StudyTask implements Serializable {
        private static final long serialVersionUID = 1L;
        String title = "", subject = "", priority = "Medium", notes = "";
        LocalDate due = LocalDate.now().plusDays(1);
        boolean done = false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}