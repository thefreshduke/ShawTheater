import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * @author $cotty $haw
 * 
 * The VideoPlayer is a MediaPlayer that plays video files. All users
 * can use it with VideoViewer.java to play any video files placed in
 * this package.
 * 
 * The audio components (player volume level and volume slider value)
 * are bound together, and the time display is formatted in the style
 * of YouTube. The visual components are not bound as tightly because
 * the play/pause button acts upon an event, whereas the player needs
 * to continuously check on its status to play or stop the video when
 * necessary.
 * 
 */
class VideoPlayer extends BorderPane {

    private static final int PADDING = 20;
    private static final int BUTTON_WIDTH = 75;
    private static final int LABEL_WIDTH = 150;

    private static final double DISABLED_SLIDER_OPACITY = 0.5;
    private static final double DOUBLE_CONVERT = 100.0;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;

    private static final String SPACE = "      ";

    private static final String MEDIA_PLAYER_BACKGROUND_COLOR = "-fx-background-color: #bbc0c4;";
    private static final String MOVIE_PANE_BACKGROUND_COLOR = "-fx-background-color: #000000;";

    private static final String PLAY_BUTTON_TEXT = "PLAY";
    private static final String PAUSE_BUTTON_TEXT = "PAUSE";
    private static final String REPLAY_BUTTON_TEXT = "REPLAY";
    private static final String MUTE_BUTTON_TEXT = "MUTE";
    private static final String UNMUTE_BUTTON_TEXT = "UNMUTE";

    private static final String VOLUME_LABEL_TEXT = "Volume: ";
    private static final String TIME_LABEL_TEXT = "%d:%02d:%02d / %d:%02d:%02d";

    private MediaPlayer myMediaPlayer;
    private MediaView myMediaView;
    private Slider myTimeSlider;
    private Label myTimeLabel;
    private Duration myDuration;
    private boolean myCycleCountIsIndefinite = false;
    private boolean mySingleReplayEnabled = false;
    private HBox myMediaBar;

    public VideoPlayer (final MediaPlayer player) {
        createMediaPlayer(player);
        defineMediaBarBehavior();

        final Button PLAY_BUTTON = new Button(PLAY_BUTTON_TEXT);
        createAndDefineVisualComponents(player, PLAY_BUTTON);
        createAndDefineAudioComponents(player);

        defineMediaPlayerBehavior(player, PLAY_BUTTON);
    }

    private void createMediaPlayer (final MediaPlayer player) {
        myMediaPlayer = player;
        setStyle(MEDIA_PLAYER_BACKGROUND_COLOR);
        myMediaView = new MediaView(player);
        Pane moviePane = new Pane() { };
        moviePane.getChildren().add(myMediaView);
        moviePane.setStyle(MOVIE_PANE_BACKGROUND_COLOR);
        setCenter(moviePane);
    }

    private void defineMediaBarBehavior () {
        myMediaBar = new HBox();
        setBottom(myMediaBar);
        BorderPane.setAlignment(myMediaBar, Pos.CENTER);
        myMediaBar.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
    }

    private void createAndDefineVisualComponents (final MediaPlayer player, final Button button) {
        button.setPrefWidth(BUTTON_WIDTH);
        button.setOnAction(event->playOrPause(player, button));
        player.currentTimeProperty().addListener(observable->verifyValues());

        myTimeSlider = new Slider();
        HBox.setHgrow(myTimeSlider, Priority.ALWAYS);
        myTimeSlider.valueProperty().addListener(observable->bindPlayerAndSliderTimes(player));

        myTimeLabel = new Label();
        myTimeLabel.setPrefWidth(LABEL_WIDTH);

        myMediaBar.getChildren().addAll(button, new Label(SPACE), myTimeSlider, myTimeLabel);
    }

    private void playOrPause (final MediaPlayer player, final Button button) {
        Status status = player.getStatus();
        if (status == Status.HALTED || status == Status.UNKNOWN) {
            return;
        }
        if (mySingleReplayEnabled) {
            mySingleReplayEnabled = false;
            player.seek(player.getStartTime());
            playVideo(player, button);
            return;
        }
        if (status == Status.PAUSED || status == Status.READY || status == Status.STOPPED) {
            player.play();
        }
        else {
            player.pause();
        }
    }

    private void bindPlayerAndSliderTimes (final MediaPlayer player) {
        if (myTimeSlider.isValueChanging()) {
            player.seek(myDuration.multiply(myTimeSlider.getValue() / DOUBLE_CONVERT));
        }
    }

    private void createAndDefineAudioComponents (final MediaPlayer player) {
        final Button VOLUME_BUTTON = new Button(MUTE_BUTTON_TEXT);
        VOLUME_BUTTON.setPrefWidth(BUTTON_WIDTH);
        myMediaBar.getChildren().addAll(VOLUME_BUTTON, new Label(SPACE));

        final Slider VOLUME_SLIDER = new Slider();
        player.volumeProperty().bind(VOLUME_SLIDER.valueProperty().divide(DOUBLE_CONVERT));
        VOLUME_BUTTON.setOnAction(event->muteOrUnmute(player, VOLUME_BUTTON, VOLUME_SLIDER));
        myMediaBar.getChildren().addAll(new Label(VOLUME_LABEL_TEXT), VOLUME_SLIDER);
    }

    private void muteOrUnmute (final MediaPlayer player, final Button button, final Slider slider) {
        if (player.isMute()) {
            player.setMute(false);
            button.setText(MUTE_BUTTON_TEXT);
            slider.setOpacity(1.0);
        }
        else {
            player.setMute(true);
            button.setText(UNMUTE_BUTTON_TEXT);
            slider.setOpacity(DISABLED_SLIDER_OPACITY);
        }
    }

    private void defineMediaPlayerBehavior (final MediaPlayer player, final Button button) {
        player.setCycleCount(myCycleCountIsIndefinite ? MediaPlayer.INDEFINITE : 1);
        player.setOnPlaying(()->playVideo(player, button));
        player.setOnPaused(()->pauseVideo(player, button));
        player.setOnReady(()->runOnReady(player));
        player.setOnEndOfMedia(()->displayReplayOption(player, button));
    }

    private void playVideo (final MediaPlayer player, final Button button) {
        player.play();
        button.setText(PAUSE_BUTTON_TEXT);
    }

    private void pauseVideo (final MediaPlayer player, final Button button) {
        player.pause();
        button.setText(PLAY_BUTTON_TEXT);
    }

    private void runOnReady (final MediaPlayer player) {
        myDuration = player.getMedia().getDuration();
        Platform.runLater(()->verifyValues());
    }

    private void displayReplayOption (final MediaPlayer player, final Button button) {
        if (!mySingleReplayEnabled) {
            mySingleReplayEnabled = true;
            player.pause();
            button.setText(REPLAY_BUTTON_TEXT);
        }
    }

    private void verifyValues () {
        Duration currentTime = myMediaPlayer.getCurrentTime();
        myTimeLabel.setText(calculateElapsedTime(currentTime, myDuration));
        myTimeSlider.setDisable(myDuration.isUnknown());

        boolean durationValid = myDuration.greaterThan(Duration.ZERO) ? true : false;
        boolean sliderActive = !myTimeSlider.isDisabled() ? true : false;
        boolean sliderValueChanging = !myTimeSlider.isValueChanging() ? true : false;

        if (durationValid && sliderActive && sliderValueChanging) {
            double duration = myDuration.toMillis();
            double doubleTime = currentTime.divide(duration).toMillis();
            double timeSliderValue = doubleTime * DOUBLE_CONVERT;
            myTimeSlider.setValue(timeSliderValue);
        }
    }

    private static String calculateElapsedTime (Duration elapsed, Duration videoDuration) {
        int seconds = (int)Math.floor(elapsed.toSeconds());

        int hours = seconds / (MINUTES_PER_HOUR * SECONDS_PER_MINUTE);
        seconds %= MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
        int minutes = seconds / SECONDS_PER_MINUTE;
        seconds %= SECONDS_PER_MINUTE;

        return formatTime(videoDuration, hours, minutes, seconds);
    }

    private static String formatTime (Duration duration, int intHours, int intMin, int intSec) {
        int seconds = (int)Math.floor(duration.toSeconds());

        int hours = seconds / (MINUTES_PER_HOUR * SECONDS_PER_MINUTE);
        seconds %= MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
        int minutes = seconds / SECONDS_PER_MINUTE;
        seconds %= SECONDS_PER_MINUTE;

        return String.format(TIME_LABEL_TEXT, intHours, intMin, intSec, hours, minutes, seconds);
    }
}
