package io.github.anirudhk_tech.janus.capabilities;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class CapabilitiesService {
    private final CapabilitiesProperties props;

    public CapabilitiesService(CapabilitiesProperties props) {
        this.props = Objects.requireNonNull(props, "props is required");
    }

    public List<CapabilitiesProperties.Source> sources() {
        return props.sources() == null ? List.of() : props.sources();
    }
}
