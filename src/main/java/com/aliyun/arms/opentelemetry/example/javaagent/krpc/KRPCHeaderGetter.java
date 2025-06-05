/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum KRPCHeaderGetter implements TextMapGetter<KRPCRequest> {
  INSTANCE;

  @Override
  public Iterable<String> keys(KRPCRequest request) {
    String tags = request.getRpcMeta().getTrace().getTags();
    if (StringUtils.isNullOrEmpty(tags)) {
      return Collections.emptyList();
    }
    String[] items = tags.split("&");
    List<String> ls = new ArrayList<>(items.length);
    for (String tag : items) {
      int idx = tag.indexOf("=");
      if (idx > 0) {
        ls.add(tag.substring(0, idx));
      }
    }
    return ls;
  }

  @Override
  public String get(KRPCRequest request, String key) {
    String tags = request.getRpcMeta().getTrace().getTags();
    if (StringUtils.isNullOrEmpty(tags)) {
      return null;
    }
    String[] items = tags.split("&");
    List<String> ls = new ArrayList<>(items.length);
    for (String tag : items) {
      int idx = tag.indexOf("=");
      if (idx > 0 && tag.substring(0, idx).equals(key)) {
        return tag.substring(idx + 1);
      }
    }
    return null;
  }
}
