import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button, Dropdown, Form, Input, Modal, Popconfirm, Select, Space, Tree, message } from "antd";
import {
  UploadOutlined,
  PlusOutlined,
  FolderOutlined,
  FileOutlined,
  DownloadOutlined,
  EditOutlined,
  DeleteOutlined,
  FileAddOutlined,
  FolderAddOutlined,
} from "@ant-design/icons";
import type { RoleFile, CreateFileRequest, UpdateFileRequest } from "../../types/entity/RoleFile";
import {
  getFiles,
  createFile,
  uploadFile,
  updateFile,
  deleteFile,
  downloadFile,
} from "../../api/roleFile";

interface RoleFilesProps {
  roleId: number;
}

export default function RoleFiles({ roleId }: RoleFilesProps) {
  const [files, setFiles] = useState<RoleFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<RoleFile | null>(null);
  const [creatingDir, setCreatingDir] = useState(false);
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const uploadParentDirRef = useRef<string>("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getFiles(roleId);
      setFiles(list);
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
    const walk = (items: RoleFile[], parentPath: string) => {
      for (const f of items) {
        if (f.isDirectory) {
          const fullPath = parentPath ? `${parentPath}/${f.name}` : f.name;
          result.push({ label: fullPath || "/", value: fullPath });
          if (f.children) walk(f.children, fullPath);
        }
      }
    };
    walk(files, "");
    return result;
  }, [files]);

  const handleCreate = (parentDir: string, isDir: boolean) => {
    setCreatingDir(isDir);
    createForm.resetFields();
    createForm.setFieldsValue({ parentDir, name: "" });
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = async () => {
    setCreating(true);
    try {
      const values = await createForm.validateFields();
      const data: CreateFileRequest = {
        parentDir: values.parentDir || "",
        name: values.name,
        targetPath: values.targetPath || undefined,
        isDirectory: creatingDir,
      };
      await createFile(roleId, data);
      message.success(creatingDir ? "目录已创建" : "文件已创建");
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

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 500 * 1024 * 1024) {
      message.error("文件大小不能超过 500MB");
      e.target.value = "";
      return;
    }

    try {
      await uploadFile(roleId, file, uploadParentDirRef.current || undefined);
      message.success("文件已上传");
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }

    e.target.value = "";
  };

  const handleEdit = (file: RoleFile) => {
    setSelectedFile(file);
    editForm.setFieldsValue({
      name: file.name,
      parentDir: file.parentDir,
      targetPath: file.targetPath || "",
    });
    setEditModalOpen(true);
  };

  const handleEditSubmit = async () => {
    setSaving(true);
    try {
      const values = await editForm.validateFields();
      if (!selectedFile) return;
      const data: UpdateFileRequest = {};
      if (values.name !== selectedFile.name) {
        data.name = values.name;
      }
      if (values.parentDir !== selectedFile.parentDir) {
        data.parentDir = values.parentDir;
      }
      if ((values.targetPath || null) !== (selectedFile.targetPath || null)) {
        data.targetPath = values.targetPath || null;
      }
      if (Object.keys(data).length === 0) {
        setEditModalOpen(false);
        return;
      }
      await updateFile(selectedFile.id, data);
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

  const handleDelete = async (fileId: number) => {
    try {
      await deleteFile(fileId);
      message.success("删除成功");
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDownload = async (fileId: number) => {
    try {
      await downloadFile(fileId);
    } catch {
      message.error("下载失败");
    }
  };

  interface TreeNode {
    key: number;
    title: React.ReactNode;
    children?: TreeNode[];
  }

  const toTreeData = (fileList: RoleFile[]): TreeNode[] =>
    fileList.map((f) => ({
      key: f.id,
      title: (
        <Space>
          {f.isDirectory ? <FolderOutlined /> : <FileOutlined />}
          <span>{f.name}</span>
          {!f.isDirectory && f.size != null && f.size > 0 && (
            <span style={{ color: "#94a3b8", fontSize: 12 }}>
              ({(f.size / 1024).toFixed(1)} KB)
            </span>
          )}
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={(e) => {
              e.stopPropagation();
              handleEdit(f);
            }}
          />
          {f.isDirectory && (
            <Dropdown
              menu={{
                items: [
                  {
                    key: "file",
                    icon: <FileAddOutlined />,
                    label: "新建文件",
                    onClick: () => {
                      const dir = f.parentDir ? `${f.parentDir}/${f.name}` : f.name;
                      handleCreate(dir, false);
                    },
                  },
                  {
                    key: "upload",
                    icon: <UploadOutlined />,
                    label: "上传文件",
                    onClick: () => {
                      const dir = f.parentDir ? `${f.parentDir}/${f.name}` : f.name;
                      triggerFileUpload(dir);
                    },
                  },
                  {
                    key: "dir",
                    icon: <FolderAddOutlined />,
                    label: "新建子目录",
                    onClick: () => {
                      const dir = f.parentDir ? `${f.parentDir}/${f.name}` : f.name;
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
          {!f.isDirectory && (
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(f.id);
              }}
            />
          )}
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(f.id)}>
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
      children: f.children ? toTreeData(f.children) : undefined,
    }));

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => handleCreate("", false)}
          >
            新建文件
          </Button>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate("", true)}>
            新建目录
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => triggerFileUpload("")}>
            上传文件
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            style={{ display: "none" }}
            onChange={handleFileSelected}
          />
        </Space>
      </div>

      {loading ? (
        <span style={{ color: "#94a3b8" }}>加载中...</span>
      ) : (
        <Tree treeData={toTreeData(files)} defaultExpandAll />
      )}

      <Modal
        title={creatingDir ? "新建目录" : "新建文件"}
        open={createModalOpen}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateModalOpen(false)}
        confirmLoading={creating}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="parentDir" label="目录">
            <Select
              options={directoryOptions}
              placeholder="选择父目录"
              allowClear
            />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input placeholder={creatingDir ? "目录名称" : "文件名称"} />
          </Form.Item>
          <Form.Item name="targetPath" label="目标路径">
            <Input placeholder="部署目标路径（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={selectedFile?.isDirectory ? "编辑目录" : "编辑文件"}
        open={editModalOpen}
        onOk={handleEditSubmit}
        onCancel={() => setEditModalOpen(false)}
        confirmLoading={saving}
        width={500}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label={selectedFile?.isDirectory ? "目录名" : "文件名"}
            rules={[{ required: true, message: "请输入名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="parentDir" label="目录">
            <Select
              options={directoryOptions}
              placeholder="选择父目录"
              allowClear
            />
          </Form.Item>
          {selectedFile && !selectedFile.isDirectory && (
            <Form.Item name="targetPath" label="目标路径">
              <Input placeholder="部署目标路径（可选）" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
