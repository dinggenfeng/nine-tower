import { Select, Typography, Button } from 'antd';
import { LinkOutlined } from '@ant-design/icons';
import { ANSIBLE_MODULES } from '../../constants/ansibleModules';

const { Text } = Typography;

interface ModuleSelectProps {
  value?: string;
  onChange?: (value: string) => void;
  filterModule?: string; // if provided, exclude this module name from the list
}

export default function ModuleSelect({ value, onChange, filterModule }: ModuleSelectProps) {
  return (
    <Select
      showSearch
      allowClear
      value={value}
      onChange={onChange}
      placeholder="选择或输入 Ansible 模块名"
      optionFilterProp="label"
      optionLabelProp="label"
    >
      {ANSIBLE_MODULES.filter((mod) => mod.name !== filterModule).map((mod) => (
        <Select.Option key={mod.name} value={mod.name} label={mod.label}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <Text strong>{mod.label}</Text>
              <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                {mod.description}
              </Text>
            </div>
            <Button
              type="link"
              size="small"
              icon={<LinkOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                window.open(mod.docUrl, '_blank', 'noopener,noreferrer');
              }}
              title="查看文档"
            />
          </div>
        </Select.Option>
      ))}
    </Select>
  );
}
