/** AUTO-GENERATED — 请勿手改。运行: node scripts/generate-twin-fallback.mjs */
export const FALLBACK_TWIN_LAYOUTS = {
  "FCW-LINE-01": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.15,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.15,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.15,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-02": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.3,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.3,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.3,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-03": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.45,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.45,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-04": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.6,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.6,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.6,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-05": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -14,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -10,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-06": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.15,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.15,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.15,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-07": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -14,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -10,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-08": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.45,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.45,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-09": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.6,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.6,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.6,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-10": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -14,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -10,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-11": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.15,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.15,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.15,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "FCW-LINE-12": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "cut_strip",
      "powder_mixing",
      "filling_forming",
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "cut_strip": {
        "x": 90,
        "y": 160,
        "label": "裁带",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 280,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "filling_forming": {
        "x": 470,
        "y": 160,
        "label": "填充成型",
        "icon": "▤"
      },
      "rough_drawing": {
        "x": 660,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 850,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 1040,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 1040,
        "y": 380,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 780,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 520,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "cut_strip": "line_speed_m_per_min",
      "powder_mixing": "mixing_rpm",
      "filling_forming": "fill_ratio_pct",
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "spool_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "cut_strip": {
        "x": -13.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -9.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "filling_forming": {
        "x": -5.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "filler"
      },
      "rough_drawing": {
        "x": -1.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": 2.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 6.3,
        "y": 0,
        "z": -2,
        "scale": [
          3,
          1.2,
          1.4
        ],
        "type": "plater"
      },
      "winding": {
        "x": 6.3,
        "y": 0,
        "z": 2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 0.3,
        "y": 0,
        "z": 2,
        "scale": [
          2.2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -5.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-01": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.15,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-02": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -10,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -5,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-03": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.45,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-04": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.6,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-05": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -10,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -5,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-06": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.15,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-07": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.3,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SW-LINE-08": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.45,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SAW-LINE-01": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.15,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "SAW-LINE-02": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "rough_drawing",
      "fine_drawing",
      "copper_plating",
      "winding",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "rough_drawing": {
        "x": 140,
        "y": 160,
        "label": "粗拔",
        "icon": "◎"
      },
      "fine_drawing": {
        "x": 380,
        "y": 160,
        "label": "细拔",
        "icon": "◎"
      },
      "copper_plating": {
        "x": 620,
        "y": 160,
        "label": "镀铜",
        "icon": "◈"
      },
      "winding": {
        "x": 860,
        "y": 160,
        "label": "层绕",
        "icon": "◐"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 680,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "rough_drawing": "tension_kN",
      "fine_drawing": "tension_kN",
      "copper_plating": "coating_thickness_top_um",
      "winding": "motor_rpm",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "rough_drawing": {
        "x": -9.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1.2,
          1.2
        ],
        "type": "drawer",
        "variant": "rough"
      },
      "fine_drawing": {
        "x": -4.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.6,
          1,
          1
        ],
        "type": "drawer",
        "variant": "fine"
      },
      "copper_plating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.8,
          1.2,
          1.4
        ],
        "type": "plater",
        "variant": "solid_wire"
      },
      "winding": {
        "x": 5.3,
        "y": 0,
        "z": -2,
        "scale": [
          1.8,
          1.6,
          1.8
        ],
        "type": "winder"
      },
      "packaging": {
        "x": 5.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-01": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -12,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -8,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-02": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.7,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-03": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.55,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-04": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.4,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-05": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -12,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -8,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-06": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.85,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-07": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.7,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-08": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.55,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-09": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.4,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-10": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -12,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -8,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-11": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.85,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-12": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.7,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-13": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.55,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-14": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.4,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-15": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -12,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -8,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-16": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.85,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.85,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.85,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.15,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.15,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.85,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-17": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.7,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.7,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.7,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.3,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.3,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.7,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-18": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.55,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.55,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.55,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.44999999999999996,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.45,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.45,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.55,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-19": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -11.4,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -7.4,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -3.4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4.6,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4.6,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -1.4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  },
  "WR-LINE-20": {
    "viewBox": {
      "w": 1280,
      "h": 620
    },
    "flowPath": [
      "wire_drawing",
      "cutting",
      "powder_mixing",
      "coating",
      "drying",
      "packaging",
      "stock_in_out"
    ],
    "nodes": {
      "wire_drawing": {
        "x": 100,
        "y": 160,
        "label": "拔丝",
        "icon": "◎"
      },
      "cutting": {
        "x": 300,
        "y": 160,
        "label": "切丝",
        "icon": "▣"
      },
      "powder_mixing": {
        "x": 500,
        "y": 160,
        "label": "配粉搅拌",
        "icon": "◉"
      },
      "coating": {
        "x": 700,
        "y": 160,
        "label": "压涂",
        "icon": "▤"
      },
      "drying": {
        "x": 900,
        "y": 160,
        "label": "烘干",
        "icon": "◈"
      },
      "packaging": {
        "x": 1040,
        "y": 380,
        "label": "包装",
        "icon": "▦"
      },
      "stock_in_out": {
        "x": 620,
        "y": 380,
        "label": "出入库",
        "icon": "⬡"
      }
    },
    "keyFields": {
      "wire_drawing": "line_speed_m_per_min",
      "cutting": "length_mm",
      "powder_mixing": "mixing_rpm",
      "coating": "coating_thickness_mm",
      "drying": "actual_temp_C",
      "packaging": "package_count",
      "stock_in_out": "location"
    },
    "positions3d": {
      "wire_drawing": {
        "x": -12,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.2,
          1.2
        ],
        "type": "drawer"
      },
      "cutting": {
        "x": -8,
        "y": 0,
        "z": -2,
        "scale": [
          2,
          1.2,
          1.4
        ],
        "type": "cutter"
      },
      "powder_mixing": {
        "x": -4,
        "y": 0,
        "z": -2,
        "scale": [
          1.6,
          1.8,
          1.6
        ],
        "type": "mixer"
      },
      "coating": {
        "x": 0,
        "y": 0,
        "z": -2,
        "scale": [
          2.4,
          1.4,
          1.6
        ],
        "type": "coater"
      },
      "drying": {
        "x": 4,
        "y": 0,
        "z": -2,
        "scale": [
          2.2,
          1.6,
          1.4
        ],
        "type": "dryer"
      },
      "packaging": {
        "x": 4,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          1.2,
          1.6
        ],
        "type": "packer"
      },
      "stock_in_out": {
        "x": -2,
        "y": 0,
        "z": 2,
        "scale": [
          2,
          0.8,
          2
        ],
        "type": "stock"
      }
    },
    "twin3dReady": true,
    "fieldLabels": {
      "line_speed_m_per_min": "线速 m/min",
      "mixing_rpm": "搅拌 rpm",
      "fill_ratio_pct": "填充率 %",
      "tension_kN": "拉力 kN",
      "coating_thickness_top_um": "镀层 μm",
      "coating_thickness_mm": "药皮 mm",
      "motor_rpm": "转速 rpm",
      "length_mm": "长度 mm",
      "actual_temp_C": "温度 °C",
      "spool_count": "包装盘数",
      "package_count": "包装数",
      "status": "状态"
    }
  }
}

export const FALLBACK_TWIN_3D_READY = {
  "FCW-LINE-01": true,
  "FCW-LINE-02": true,
  "FCW-LINE-03": true,
  "FCW-LINE-04": true,
  "FCW-LINE-05": true,
  "FCW-LINE-06": true,
  "FCW-LINE-07": true,
  "FCW-LINE-08": true,
  "FCW-LINE-09": true,
  "FCW-LINE-10": true,
  "FCW-LINE-11": true,
  "FCW-LINE-12": true,
  "SW-LINE-01": true,
  "SW-LINE-02": true,
  "SW-LINE-03": true,
  "SW-LINE-04": true,
  "SW-LINE-05": true,
  "SW-LINE-06": true,
  "SW-LINE-07": true,
  "SW-LINE-08": true,
  "SAW-LINE-01": true,
  "SAW-LINE-02": true,
  "WR-LINE-01": true,
  "WR-LINE-02": true,
  "WR-LINE-03": true,
  "WR-LINE-04": true,
  "WR-LINE-05": true,
  "WR-LINE-06": true,
  "WR-LINE-07": true,
  "WR-LINE-08": true,
  "WR-LINE-09": true,
  "WR-LINE-10": true,
  "WR-LINE-11": true,
  "WR-LINE-12": true,
  "WR-LINE-13": true,
  "WR-LINE-14": true,
  "WR-LINE-15": true,
  "WR-LINE-16": true,
  "WR-LINE-17": true,
  "WR-LINE-18": true,
  "WR-LINE-19": true,
  "WR-LINE-20": true
}
