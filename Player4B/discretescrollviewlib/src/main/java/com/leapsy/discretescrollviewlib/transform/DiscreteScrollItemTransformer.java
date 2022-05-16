package com.leapsy.discretescrollviewlib.transform;

import android.view.View;

public interface DiscreteScrollItemTransformer {
    void transformItem(View item, float position);
}
