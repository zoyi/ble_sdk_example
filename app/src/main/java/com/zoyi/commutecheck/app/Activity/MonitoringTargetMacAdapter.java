package com.zoyi.commutecheck.app.Activity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.zoyi.commutecheck.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Created by huy on 2019-05-27.
 */
public class MonitoringTargetMacAdapter extends RecyclerView.Adapter<MonitoringTargetMacAdapter.ViewHolder> {

  private List<Pair<String, List<Integer>>> monitoringTargetMacRssis;

  public MonitoringTargetMacAdapter(Map<String, List<Integer>> monitoringTargetMacRssis) {
    this.setDataSet(monitoringTargetMacRssis);
  }

  public void setDataSet(Map<String, List<Integer>> monitoringTargetMacRssis){
    this.monitoringTargetMacRssis = new ArrayList<>();
    for (Map.Entry<String, List<Integer>> entry  : monitoringTargetMacRssis.entrySet()) {
      this.monitoringTargetMacRssis.add(new Pair<>(entry.getKey(), entry.getValue()));
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.monitoring_target_mac_item, parent, false);
    return new MonitoringTargetMacAdapter.ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
    Pair<String, List<Integer>> value = this.monitoringTargetMacRssis.get(i);
    viewHolder.onBind(value);
  }

  @Override
  public int getItemCount() {
    return monitoringTargetMacRssis.size();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    TextView targetMac;
    TextView rssi;

    ViewHolder(View itemView) {
      super(itemView);
      targetMac = itemView.findViewById(R.id.monitoring_target_mac_item_target_mac);
      rssi = itemView.findViewById(R.id.monitoring_target_mac_item_rssi);
    }

    void onBind(Pair<String, List<Integer>> value) {
      targetMac.setText(value.first);
      String rssiString = String.format("마지막: %d, 평균: %d", value.second.get(value.second.size() - 1), average(value.second));
      rssi.setText(rssiString);
    }

    Long average(List<Integer> list) {
      Long sum = 0L;
      for (Integer value : list) {
        sum += value;
      }
      return sum / list.size();
    }
  }
}
