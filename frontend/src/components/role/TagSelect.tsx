import { useEffect, useState } from "react";
import { Select } from "antd";
import { useParams } from "react-router-dom";
import type { Tag } from "../../types/entity/Tag";
import { listTags } from "../../api/tag";

interface TagSelectProps {
  value?: number[];
  onChange?: (value: number[]) => void;
}

export default function TagSelect({ value, onChange }: TagSelectProps) {
  const { id: projectId } = useParams<{ id: string }>();
  const [tags, setTags] = useState<Tag[]>([]);

  useEffect(() => {
    const pid = Number(projectId);
    if (!pid) return;
    listTags(pid).then(setTags);
  }, [projectId]);

  return (
    <Select
      mode="multiple"
      value={value}
      onChange={onChange}
      placeholder="选择标签"
      options={tags.map((t) => ({ label: t.name, value: t.id }))}
      allowClear
    />
  );
}
