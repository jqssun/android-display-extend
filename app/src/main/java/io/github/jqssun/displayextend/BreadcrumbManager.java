package io.github.jqssun.displayextend;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class BreadcrumbManager {
    private LinearLayout breadcrumb;
    private List<String> navigationPath = new ArrayList<>();
    private FragmentManager fragmentManager;
    private FragmentFactory currentFragmentFactory;

    public BreadcrumbManager(Context context, FragmentManager fragmentManager, LinearLayout breadcrumb) {
        this.breadcrumb = breadcrumb;
        this.fragmentManager = fragmentManager;
    }

    public void pushBreadcrumb(String newPath, FragmentFactory fragmentFactory) {
        try {
            if (!newPath.isEmpty() && !navigationPath.contains(newPath)) {
                navigationPath.add(newPath);
            }
            updateBreadcrumbView();
            this.currentFragmentFactory = fragmentFactory;
            Fragment fragment = fragmentFactory.createFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Throwable e) {
            // ignore
        }
    }

    public void popBreadcrumb() {
        try {
            if (navigationPath.size() > 1) {
                navigationPath.remove(navigationPath.size() - 1);
            } else {
                ((MainActivity) fragmentManager.findFragmentById(R.id.fragmentContainer).getActivity()).finish();
            }
            updateBreadcrumbView();
            fragmentManager.popBackStack();
        } catch (Exception e) {
            // ignore
        }
    }

    private void updateBreadcrumbView() {
        breadcrumb.removeAllViews();

        for (int i = 0; i < navigationPath.size(); i++) {
            TextView separator = new TextView(breadcrumb.getContext());
            separator.setText(" > ");
            breadcrumb.addView(separator);

            TextView pathView = new TextView(breadcrumb.getContext());
            pathView.setText(navigationPath.get(i));
            pathView.setTextColor(breadcrumb.getContext().getResources().getColor(R.color.blue));
            final int index = i;
            pathView.setClickable(true);
            pathView.setOnClickListener(v -> {
                while (navigationPath.size() > index + 1) {
                    popBreadcrumb();
                }
            });
            breadcrumb.addView(pathView);
        }
    }

    public LinearLayout getBreadcrumbView() {
        return breadcrumb;
    }

    public interface FragmentFactory {
        Fragment createFragment();
    }

    public void refreshCurrentFragment() {
        try {
            if (State.currentActivity.get() == null) {
                return;
            }
            if (currentFragmentFactory != null) {
                fragmentManager.popBackStack();

                Fragment fragment = currentFragmentFactory.createFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}