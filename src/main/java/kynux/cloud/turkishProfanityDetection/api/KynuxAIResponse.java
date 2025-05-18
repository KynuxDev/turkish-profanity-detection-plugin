package kynux.cloud.turkishProfanityDetection.api;

import com.google.gson.annotations.SerializedName;

public class KynuxAIResponse {

    @SerializedName("is_profane")
    private boolean isProfane;

    @SerializedName("is_safe_for_minecraft")
    private boolean isSafeForMinecraft;

    @SerializedName("severity")
    private int severity;

    @SerializedName("category")
    private String category;

    @SerializedName("detected_word")
    private String detectedWord;

    @SerializedName("action_recommendation")
    private String actionRecommendation;

    @SerializedName("analysis_details")
    private String analysisDetails;

    public boolean isProfane() {
        return isProfane;
    }

    public boolean isSafeForMinecraft() {
        return isSafeForMinecraft;
    }

    public int getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    public String getDetectedWord() {
        return detectedWord;
    }

    public String getActionRecommendation() {
        return actionRecommendation;
    }

    public String getAnalysisDetails() {
        return analysisDetails;
    }

    public void setProfane(boolean profane) {
        isProfane = profane;
    }

    public void setSafeForMinecraft(boolean safeForMinecraft) {
        isSafeForMinecraft = safeForMinecraft;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDetectedWord(String detectedWord) {
        this.detectedWord = detectedWord;
    }

    public void setActionRecommendation(String actionRecommendation) {
        this.actionRecommendation = actionRecommendation;
    }

    public void setAnalysisDetails(String analysisDetails) {
        this.analysisDetails = analysisDetails;
    }

    @Override
    public String toString() {
        return "KynuxAIResponse{" +
                "isProfane=" + isProfane +
                ", isSafeForMinecraft=" + isSafeForMinecraft +
                ", severity=" + severity +
                ", category='" + category + '\'' +
                ", detectedWord='" + detectedWord + '\'' +
                ", actionRecommendation='" + actionRecommendation + '\'' +
                ", analysisDetails='" + analysisDetails + '\'' +
                '}';
    }
}
