import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Dropdown,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tree,
  message,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ArrowLeftOutlined,
  FolderOutlined,
  DownloadOutlined,
  UploadOutlined,
  FileAddOutlined,
  FolderAddOutlined,
} from "@ant-design/icons";
import CodeMirror from "@uiw/react-codemirror";
import { yaml } from "@codemirror/lang-yaml";
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from "../../types/entity/Template";
import {
  createTemplate,
  getTemplates,
  getTemplate,
  updateTemplate,
  deleteTemplate,
  downloadTemplate,
} from "../../api/template";

interface RoleTemplatesProps {
  roleId: number;
}

export default function RoleTemplates({ roleId }: RoleTemplatesProps) {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<Template | null>(null);
  const [creatingDir, setCreatingDir] = useState(false);
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [contentTemplate, setContentTemplate] = useState<Template | null>(null);
  const [contentValue, setContentValue] = useState("");
  const [savingContent, setSavingContent] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const uploadParentDirRef = useRef<string>("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getTemplates(roleId);
      setTemplates(list);
    } catch {
      // ignore fetch errors
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const directoryOptions = useMemo(() => {
    const result: { label: string; value: string }[] = [{ label: "/ (根目录)", value: "" }];
    const walk = (items: Template[], parentPath: string) => {
      for (const t of items) {
        if (t.isDirectory) {
          const fullPath = parentPath ? `${parentPath}/${t.name}` : t.name;
          result.push({ label: fullPath || "/", value: fullPath });
          if (t.children) walk(t.children, fullPath);
        }
      }
    };
    walk(templates, "");
    return result;
  }, [templates]);

  const handleCreate = (parentDir: string, isDir: boolean) => {
    setEditingTemplate(null);
    setCreatingDir(isDir);
    createForm.resetFields();
    createForm.setFieldsValue({ parentDir, name: "" });
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = async () => {
    setCreating(true);
    try {
      const values = await createForm.validateFields();
      const data: CreateTemplateRequest = {
        name: values.name,
        parentDir: values.parentDir || "",
        targetPath: values.targetPath || undefined,
        content: creatingDir ? undefined : values.content,
        isDirectory: creatingDir,
      };
      await createTemplate(roleId, data);
      message.success(creatingDir ? "目录已创建" : "模板已创建");
      setCreateModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    } finally {
      setCreating(false);
    }
  };

  const triggerFileUpload = (parentDir: string) => {
    uploadParentDirRef.current = parentDir;
    fileInputRef.current?.click();
  };

  const [uploading, setUploading] = useState(false);

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fileList = e.target.files;
    if (!fileList || fileList.length === 0) return;

    const parentDir = uploadParentDirRef.current || "";
    const files = Array.from(fileList);

    setUploading(true);
    const hideLoading = message.loading(`正在上传 ${files.length} 个模板...`, 0);

    let success = 0;
    let fail = 0;

    const readFile = (file: File): Promise<string> =>
      new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject(new Error(`读取 ${file.name} 失败`));
        reader.readAsText(file);
      });

    for (const file of files) {
      try {
        const content = await readFile(file);
        await createTemplate(roleId, { name: file.name, parentDir, content });
        success++;
      } catch (err) {
        fail++;
      }
    }

    hideLoading();
    setUploading(false);
    e.target.value = "";

    if (fail === 0) {
      message.success(`${success} 个模板已上传`);
    } else {
      message.warning(`上传完成: ${success} 成功, ${fail} 失败`);
    }
    fetchData();
  };

  const handleEdit = (template: Template) => {
    setEditingTemplate(template);
    editForm.setFieldsValue({
      name: template.name,
      parentDir: template.parentDir || "",
      targetPath: template.targetPath || "",
    });
    setEditModalOpen(true);
  };

  const handleEditSubmit = async () => {
    setSaving(true);
    try {
      const values = await editForm.validateFields();
      if (!editingTemplate) return;
      const data: UpdateTemplateRequest = {};
      if (values.name !== editingTemplate.name) {
        data.name = values.name;
      }
      if ((values.parentDir || "") !== (editingTemplate.parentDir || "")) {
        data.parentDir = values.parentDir || "";
      }
      if ((values.targetPath || null) !== (editingTemplate.targetPath || null)) {
        data.targetPath = values.targetPath || null;
      }
      if (Object.keys(data).length === 0) {
        setEditModalOpen(false);
        return;
      }
      await updateTemplate(editingTemplate.id, data);
      message.success("更新成功");
      setEditModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTemplate(id);
      message.success("删除成功");
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleEditContent = async (tpl: Template) => {
    try {
      const detail = await getTemplate(tpl.id);
      setContentTemplate(detail);
      setContentValue(detail.content || "");
    } catch {
      message.error("加载模板内容失败");
    }
  };

  const handleSaveContent = async () => {
    if (!contentTemplate) return;
    setSavingContent(true);
    try {
      await updateTemplate(contentTemplate.id, { content: contentValue });
      message.success("模板内容已保存");
      setContentTemplate(null);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    } finally {
      setSavingContent(false);
    }
  };

  const handleDownload = async (tpl: Template) => {
    try {
      await downloadTemplate(tpl.id);
    } catch {
      message.error("下载失败");
    }
  };

  // --- Content editor view (CodeMirror) ---
  if (contentTemplate) {
    return (
      <div>
        <div
          style={{
            marginBottom: 16,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => setContentTemplate(null)}
            style={{ color: "#64748b", padding: "4px 8px" }}
          >
            返回模板列表
          </Button>
          <Button type="primary" onClick={handleSaveContent} loading={savingContent}>
            保存
          </Button>
        </div>
        <div
          style={{
            marginBottom: 16,
            padding: "12px 16px",
            border: "1px solid #f0f0f0",
            borderRadius: 8,
          }}
        >
          <Space>
            <FileTextOutlined />
            <span>
              {contentTemplate.parentDir ? `${contentTemplate.parentDir}/` : ""}
              {contentTemplate.name}
            </span>
          </Space>
          <span style={{ marginLeft: 16, color: "#64748b" }}>
            目标路径: {contentTemplate.targetPath || "未设置"}
          </span>
        </div>
        <CodeMirror
          value={contentValue}
          onChange={(value) => setContentValue(value)}
          extensions={[yaml()]}
          height="500px"
          basicSetup={{
            lineNumbers: true,
            highlightActiveLine: true,
            bracketMatching: true,
          }}
        />
      </div>
    );
  }

  // --- Tree view ---
  interface TreeNode {
    key: string;
    title: React.ReactNode;
    children?: TreeNode[];
  }

  const toTreeData = (items: Template[]): TreeNode[] =>
    items.map((t) => ({
      key: t.isDirectory ? `dir:${t.parentDir || ""}/${t.name}` : `file:${t.id}`,
      title: (
        <Space>
          {t.isDirectory ? <FolderOutlined /> : <FileTextOutlined />}
          <span>{t.name}</span>
          {!t.isDirectory && t.size != null && t.size > 0 && (
            <span style={{ color: "#94a3b8", fontSize: 12 }}>
              ({(t.size / 1024).toFixed(1)} KB)
            </span>
          )}
          {!t.isDirectory && (
            <Button
              type="link"
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                handleEditContent(t);
              }}
            >
              内容
            </Button>
          )}
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={(e) => {
              e.stopPropagation();
              handleEdit(t);
            }}
          />
          {t.isDirectory && (
            <Dropdown
              menu={{
                items: [
                  {
                    key: "file",
                    icon: <FileAddOutlined />,
                    label: "新建模板",
                    onClick: () => {
                      const dir = t.parentDir ? `${t.parentDir}/${t.name}` : t.name;
                      handleCreate(dir, false);
                    },
                  },
                  {
                    key: "upload",
                    icon: <UploadOutlined />,
                    label: "上传模板",
                    onClick: () => {
                      const dir = t.parentDir ? `${t.parentDir}/${t.name}` : t.name;
                      triggerFileUpload(dir);
                    },
                  },
                  {
                    key: "dir",
                    icon: <FolderAddOutlined />,
                    label: "新建子目录",
                    onClick: () => {
                      const dir = t.parentDir ? `${t.parentDir}/${t.name}` : t.name;
                      handleCreate(dir, true);
                    },
                  },
                ],
              }}
              trigger={["click"]}
            >
              <Button
                type="link"
                size="small"
                icon={<PlusOutlined />}
                onClick={(e) => e.stopPropagation()}
              />
            </Dropdown>
          )}
          {!t.isDirectory && (
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(t);
              }}
            />
          )}
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(t.id)}>
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={(e) => e.stopPropagation()}
            />
          </Popconfirm>
        </Space>
      ),
      children: t.children ? toTreeData(t.children) : undefined,
    }));

  const treeData = toTreeData(templates);

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleCreate("", false)}>
            新建模板
          </Button>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate("", true)}>
            新建目录
          </Button>
          <Button
            icon={<UploadOutlined />}
            onClick={() => triggerFileUpload("")}
            loading={uploading}
          >
            上传模板
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            style={{ display: "none" }}
            onChange={handleFileSelected}
          />
        </Space>
      </div>

      {loading ? (
        <span style={{ color: "#94a3b8" }}>加载中...</span>
      ) : (
        <Tree treeData={treeData} defaultExpandAll />
      )}

      <Modal
        title={creatingDir ? "新建目录" : editingTemplate ? "编辑模板" : "新建模板"}
        open={createModalOpen}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateModalOpen(false)}
        confirmLoading={creating}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="parentDir" label="目录">
            <Select options={directoryOptions} placeholder="选择父目录" allowClear />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input placeholder={creatingDir ? "目录名称" : "模板名称"} />
          </Form.Item>
          <Form.Item name="targetPath" label="目标路径">
            <Input placeholder="部署目标路径（可选）" />
          </Form.Item>
          {!creatingDir && (
            <Form.Item name="content" label="模板内容">
              <Input.TextArea rows={8} placeholder="模板内容（可选，创建后可在线编辑）" />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title={editingTemplate?.isDirectory ? "编辑目录" : "编辑模板"}
        open={editModalOpen}
        onOk={handleEditSubmit}
        onCancel={() => setEditModalOpen(false)}
        confirmLoading={saving}
        width={500}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label={editingTemplate?.isDirectory ? "目录名" : "模板名"}
            rules={[{ required: true, message: "请输入名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="parentDir" label="目录">
            <Select options={directoryOptions} placeholder="选择父目录" allowClear />
          </Form.Item>
          {editingTemplate && !editingTemplate.isDirectory && (
            <Form.Item name="targetPath" label="目标路径">
              <Input placeholder="部署目标路径（可选）" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
