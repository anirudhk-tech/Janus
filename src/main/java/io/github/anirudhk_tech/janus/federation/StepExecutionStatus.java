package io.github.anirudhk_tech.janus.federation;

public enum StepExecutionStatus {
    SUCCESS,
    FAILURE,
    TIMEOUT;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
