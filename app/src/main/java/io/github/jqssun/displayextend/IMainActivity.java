package io.github.jqssun.displayextend;

import androidx.fragment.app.Fragment;

public interface IMainActivity {
    void updateLogs();
    void navigateToDetail(Fragment fragment);
    void refreshCurrentFragment();
}
