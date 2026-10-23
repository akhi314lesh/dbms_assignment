package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.HomeAccessId;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.HomeAccessRepository;
import com.smarthome.smart_home_backend.repository.HomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HomeService {

    private final HomeRepository homeRepository;
    private final HomeAccessRepository homeAccessRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public HomeService(HomeRepository homeRepository, HomeAccessRepository homeAccessRepository, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.homeRepository = homeRepository;
        this.homeAccessRepository = homeAccessRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Home> getAllHomes() {
        return homeRepository.findAll();
    }

    public List<Home> getHomesByIds(Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return homeRepository.findByHomeIdIn(homeIds);
    }

    public Optional<Home> getHomeById(Long id) {
        return homeRepository.findById(id);
    }

    @Transactional
    public Home createHome(Home home, User creator) {
        Home savedHome = homeRepository.save(home);

        if (creator != null && creator.getUserId() != null) {
            HomeAccessId accessId = new HomeAccessId(creator.getUserId(), savedHome.getHomeId());
            HomeAccess access = new HomeAccess();
            access.setId(accessId);
            access.setUser(creator);
            access.setHome(savedHome);
            access.setRole("OWNER");
            access.setDateGranted(LocalDate.now());
            homeAccessRepository.save(access);
        }

        return savedHome;
    }

    @Transactional
    public Home createHome(Home home) {
        return createHome(home, null);
    }

    @Transactional
    public Home updateHome(Long id, Home homeDetails) {
        Home home = homeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Home not found"));

        home.setHomeName(homeDetails.getHomeName());
        home.setStreet(homeDetails.getStreet());
        home.setCity(homeDetails.getCity());
        home.setPincode(homeDetails.getPincode());

        return homeRepository.save(home);
    }

    @Transactional
    public void deleteHome(Long id) {
        homeRepository.deleteById(id);
    }

    public String getHealthReport(Long homeId) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            java.sql.CallableStatement enable = null;
            java.sql.CallableStatement call = null;
            java.sql.CallableStatement getLine = null;
            java.sql.CallableStatement disable = null;
            try {
                enable = conn.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(1000000); END;");
                enable.execute();

                call = conn.prepareCall("BEGIN sp_home_device_health_report(?); END;");
                call.setLong(1, homeId);
                call.execute();

                getLine = conn.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;");
                getLine.registerOutParameter(1, java.sql.Types.VARCHAR);
                getLine.registerOutParameter(2, java.sql.Types.NUMERIC);

                StringBuilder report = new StringBuilder();
                while (true) {
                    getLine.execute();
                    int status = getLine.getInt(2);
                    if (status != 0) break;
                    report.append(getLine.getString(1)).append("\n");
                }

                disable = conn.prepareCall("BEGIN DBMS_OUTPUT.DISABLE(); END;");
                disable.execute();

                return report.toString();
            } finally {
                if (enable != null) enable.close();
                if (call != null) call.close();
                if (getLine != null) getLine.close();
                if (disable != null) disable.close();
            }
        });
    }
}