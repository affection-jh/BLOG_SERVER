import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:image_picker/image_picker.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:io';

void main() {
  runApp(BlogEditorApp());
}

class BlogEditorApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '노션 스타일 블로그 에디터',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        fontFamily: 'NotoSansKR',
        scaffoldBackgroundColor: Colors.white,
      ),
      home: BlogEditorScreen(),
    );
  }
}

class BlogEditorScreen extends StatefulWidget {
  @override
  _BlogEditorScreenState createState() => _BlogEditorScreenState();
}

class _BlogEditorScreenState extends State<BlogEditorScreen> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _contentController = TextEditingController();
  final TextEditingController _tagsController = TextEditingController();

  String _selectedStatus = 'DRAFT';
  String _selectedAccessLevel = 'PUBLIC';
  List<String> _tags = [];
  List<BlogImage> _images = [];
  bool _isLoading = false;

  final ImagePicker _picker = ImagePicker();

  // API 설정
  static const String baseUrl = 'http://localhost:8080/api';
  static const String userId = 'user123'; // 실제로는 로그인 시스템에서 가져와야 함

  @override
  void initState() {
    super.initState();
    _loadDraft();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    _tagsController.dispose();
    super.dispose();
  }

  // 드래프트 로드
  Future<void> _loadDraft() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final draftJson = prefs.getString('blog_draft');
      if (draftJson != null) {
        final draft = json.decode(draftJson);
        setState(() {
          _titleController.text = draft['title'] ?? '';
          _contentController.text = draft['content'] ?? '';
          _tags = List<String>.from(draft['tags'] ?? []);
          _selectedStatus = draft['status'] ?? 'DRAFT';
          _selectedAccessLevel = draft['accessLevel'] ?? 'PUBLIC';
        });
      }
    } catch (e) {
      print('드래프트 로드 실패: $e');
    }
  }

  // 드래프트 저장
  Future<void> _saveDraft() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final draft = {
        'title': _titleController.text,
        'content': _contentController.text,
        'tags': _tags,
        'status': _selectedStatus,
        'accessLevel': _selectedAccessLevel,
        'timestamp': DateTime.now().toIso8601String(),
      };
      await prefs.setString('blog_draft', json.encode(draft));
      _showSnackBar('드래프트가 저장되었습니다.');
    } catch (e) {
      _showSnackBar('드래프트 저장 실패: $e');
    }
  }

  // 이미지 업로드
  Future<void> _pickImage() async {
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,
        maxHeight: 1080,
        imageQuality: 85,
      );

      if (image != null) {
        setState(() {
          _isLoading = true;
        });

        // 서버에 이미지 업로드
        final uploadResult = await _uploadImageToServer(image);

        if (uploadResult != null) {
          setState(() {
            _images.add(uploadResult);
            _isLoading = false;
          });
          _showSnackBar('이미지가 업로드되었습니다.');
        } else {
          setState(() {
            _isLoading = false;
          });
          _showSnackBar('이미지 업로드에 실패했습니다.');
        }
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showSnackBar('이미지 업로드 실패: $e');
    }
  }

  // 서버에 이미지 업로드
  Future<BlogImage?> _uploadImageToServer(XFile image) async {
    try {
      final file = File(image.path);
      final bytes = await file.readAsBytes();

      var request = http.MultipartRequest(
        'POST',
        Uri.parse('$baseUrl/images/upload'),
      );

      request.headers['Authorization'] = userId;
      request.files.add(
        http.MultipartFile.fromBytes(
          'file',
          bytes,
          filename: image.name,
        ),
      );

      final response = await request.send();
      final responseData = await response.stream.bytesToString();

      if (response.statusCode == 200) {
        final jsonData = json.decode(responseData);
        return BlogImage(
          id: jsonData['imageId'],
          localPath: image.path,
          fileName: jsonData['fileName'],
          fileSize: jsonData['fileSize'],
          uploadTime: DateTime.now(),
          serverImageId: jsonData['imageId'],
          status: jsonData['status'],
        );
      } else {
        print('이미지 업로드 실패: ${response.statusCode} - $responseData');
        return null;
      }
    } catch (e) {
      print('이미지 업로드 에러: $e');
      return null;
    }
  }

  // 태그 추가
  void _addTag() {
    final tag = _tagsController.text.trim();
    if (tag.isNotEmpty && !_tags.contains(tag)) {
      setState(() {
        _tags.add(tag);
        _tagsController.clear();
      });
    }
  }

  // 태그 삭제
  void _removeTag(String tag) {
    setState(() {
      _tags.remove(tag);
    });
  }

  // 블로그 생성 (서버 API 호출)
  Future<Map<String, dynamic>?> _createBlog() async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/blogs'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': userId,
        },
        body: json.encode({
          'title': _titleController.text.trim(),
          'content': _contentController.text.trim(),
          'tags': _tags,
          'status': _selectedStatus,
          'accessLevel': _selectedAccessLevel,
          'selectedThumbnailImageId':
              _images.isNotEmpty ? _images.first.serverImageId : null,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return json.decode(response.body);
      } else {
        print('블로그 생성 실패: ${response.statusCode} - ${response.body}');
        return null;
      }
    } catch (e) {
      print('블로그 생성 에러: $e');
      return null;
    }
  }

  // 블로그 발행
  Future<void> _publishBlog() async {
    if (_titleController.text.trim().isEmpty) {
      _showSnackBar('제목을 입력해주세요.');
      return;
    }

    if (_contentController.text.trim().isEmpty) {
      _showSnackBar('내용을 입력해주세요.');
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      // 1. 먼저 블로그 생성 (DRAFT 상태)
      final blogData = await _createBlog();

      if (blogData != null) {
        final blogId = blogData['id'];

        // 2. 발행 상태로 변경
        if (_selectedStatus == 'PUBLISHED') {
          final publishResponse = await http.post(
            Uri.parse('$baseUrl/blogs/$blogId/publish'),
            headers: {
              'Authorization': userId,
            },
          );

          if (publishResponse.statusCode == 200) {
            final publishedBlog = json.decode(publishResponse.body);
            blogData.addAll(publishedBlog);
          }
        }

        // 3. 이미지 확정 (사용된 이미지들)
        for (final image in _images) {
          if (image.serverImageId != null) {
            await http.post(
              Uri.parse('$baseUrl/images/${image.serverImageId}/confirm'),
              headers: {
                'Authorization': userId,
              },
            );
          }
        }

        // 4. 발행 성공 후 드래프트 삭제
        final prefs = await SharedPreferences.getInstance();
        await prefs.remove('blog_draft');

        setState(() {
          _isLoading = false;
        });

        _showSuccessDialog(blogData);
      } else {
        setState(() {
          _isLoading = false;
        });
        _showSnackBar('블로그 생성에 실패했습니다.');
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showSnackBar('블로그 발행 실패: $e');
    }
  }

  // 블로그 가져오기 (내 블로그 목록에서 선택)
  Future<void> _importBlog() async {
    try {
      // 내 블로그 목록 조회
      final response = await http.get(
        Uri.parse('$baseUrl/blogs/my?page=0&size=10'),
        headers: {
          'Authorization': userId,
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final blogs = data['content'] as List;

        if (blogs.isNotEmpty) {
          // 첫 번째 블로그를 가져오기 (실제로는 선택 다이얼로그를 띄워야 함)
          final importedBlog = blogs.first;

          setState(() {
            _titleController.text = importedBlog['title'] ?? '';
            _contentController.text = importedBlog['content'] ?? '';
            _tags = List<String>.from(importedBlog['tags'] ?? []);
            _selectedStatus = importedBlog['status'] ?? 'DRAFT';
            _selectedAccessLevel = importedBlog['accessLevel'] ?? 'PUBLIC';
          });

          _showSnackBar('블로그를 가져왔습니다.');
        } else {
          _showSnackBar('가져올 블로그가 없습니다.');
        }
      } else {
        _showSnackBar('블로그 목록 조회에 실패했습니다.');
      }
    } catch (e) {
      _showSnackBar('블로그 가져오기 실패: $e');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: Duration(seconds: 2)),
    );
  }

  void _showSuccessDialog(Map<String, dynamic> blogData) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('발행 완료!'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('블로그가 성공적으로 발행되었습니다.'),
            SizedBox(height: 16),
            Text('제목: ${blogData['title']}'),
            Text('상태: ${blogData['status']}'),
            Text('접근 권한: ${blogData['accessLevel']}'),
            Text('태그: ${blogData['tags'].join(', ')}'),
            Text('이미지: ${_images.length}개'),
            Text('블로그 ID: ${blogData['id']}'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _clearForm();
            },
            child: Text('새 글 작성'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
            },
            child: Text('확인'),
          ),
        ],
      ),
    );
  }

  void _clearForm() {
    setState(() {
      _titleController.clear();
      _contentController.clear();
      _tags.clear();
      _images.clear();
      _selectedStatus = 'DRAFT';
      _selectedAccessLevel = 'PUBLIC';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('노션 스타일 블로그 에디터'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 1,
        actions: [
          IconButton(
            icon: Icon(Icons.save),
            onPressed: _saveDraft,
            tooltip: '드래프트 저장',
          ),
          IconButton(
            icon: Icon(Icons.file_download),
            onPressed: _importBlog,
            tooltip: '블로그 가져오기',
          ),
          ElevatedButton(
            onPressed: _isLoading ? null : _publishBlog,
            child: _isLoading
                ? SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text('발행'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
          SizedBox(width: 16),
        ],
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 제목 입력
                  _buildTitleSection(),
                  SizedBox(height: 24),

                  // 태그 입력
                  _buildTagsSection(),
                  SizedBox(height: 24),

                  // 설정 섹션
                  _buildSettingsSection(),
                  SizedBox(height: 24),

                  // 이미지 업로드
                  _buildImageSection(),
                  SizedBox(height: 24),

                  // 내용 입력
                  _buildContentSection(),
                  SizedBox(height: 24),

                  // 미리보기
                  _buildPreviewSection(),
                ],
              ),
            ),
    );
  }

  Widget _buildTitleSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '제목',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.grey[800],
          ),
        ),
        SizedBox(height: 8),
        TextField(
          controller: _titleController,
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            hintText: '블로그 제목을 입력하세요...',
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: Colors.grey[300]!),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: Colors.blue, width: 2),
            ),
            filled: true,
            fillColor: Colors.grey[50],
            contentPadding: EdgeInsets.all(16),
          ),
        ),
      ],
    );
  }

  Widget _buildTagsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '태그',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.grey[800],
          ),
        ),
        SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _tagsController,
                decoration: InputDecoration(
                  hintText: '태그를 입력하고 Enter를 누르세요',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: BorderSide(color: Colors.grey[300]!),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: BorderSide(color: Colors.blue, width: 2),
                  ),
                  filled: true,
                  fillColor: Colors.grey[50],
                  contentPadding: EdgeInsets.all(12),
                ),
                onSubmitted: (_) => _addTag(),
              ),
            ),
            SizedBox(width: 8),
            ElevatedButton(
              onPressed: _addTag,
              child: Text('추가'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
        if (_tags.isNotEmpty) ...[
          SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _tags
                .map(
                  (tag) => Chip(
                    label: Text(tag),
                    deleteIcon: Icon(Icons.close, size: 18),
                    onDeleted: () => _removeTag(tag),
                    backgroundColor: Colors.blue[50],
                    deleteIconColor: Colors.blue[700],
                  ),
                )
                .toList(),
          ),
        ],
      ],
    );
  }

  Widget _buildSettingsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '설정',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.grey[800],
          ),
        ),
        SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('상태', style: TextStyle(fontWeight: FontWeight.w500)),
                  SizedBox(height: 4),
                  DropdownButtonFormField<String>(
                    value: _selectedStatus,
                    decoration: InputDecoration(
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: BorderSide(color: Colors.grey[300]!),
                      ),
                      filled: true,
                      fillColor: Colors.grey[50],
                      contentPadding: EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                    ),
                    items: [
                      DropdownMenuItem(value: 'DRAFT', child: Text('임시저장')),
                      DropdownMenuItem(value: 'PUBLISHED', child: Text('발행')),
                      DropdownMenuItem(value: 'ARCHIVED', child: Text('보관')),
                    ],
                    onChanged: (value) {
                      setState(() {
                        _selectedStatus = value!;
                      });
                    },
                  ),
                ],
              ),
            ),
            SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('접근 권한', style: TextStyle(fontWeight: FontWeight.w500)),
                  SizedBox(height: 4),
                  DropdownButtonFormField<String>(
                    value: _selectedAccessLevel,
                    decoration: InputDecoration(
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: BorderSide(color: Colors.grey[300]!),
                      ),
                      filled: true,
                      fillColor: Colors.grey[50],
                      contentPadding: EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                    ),
                    items: [
                      DropdownMenuItem(value: 'PUBLIC', child: Text('공개')),
                      DropdownMenuItem(value: 'PRIVATE', child: Text('비공개')),
                      DropdownMenuItem(value: 'SHARED', child: Text('공유')),
                      DropdownMenuItem(value: 'PASSWORD', child: Text('비밀번호')),
                    ],
                    onChanged: (value) {
                      setState(() {
                        _selectedAccessLevel = value!;
                      });
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildImageSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '이미지',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.grey[800],
              ),
            ),
            ElevatedButton.icon(
              onPressed: _pickImage,
              icon: Icon(Icons.add_photo_alternate),
              label: Text('이미지 추가'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
        SizedBox(height: 12),
        if (_images.isEmpty)
          Container(
            width: double.infinity,
            height: 120,
            decoration: BoxDecoration(
              border: Border.all(
                color: Colors.grey[300]!,
                style: BorderStyle.solid,
              ),
              borderRadius: BorderRadius.circular(8),
              color: Colors.grey[50],
            ),
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.image, size: 48, color: Colors.grey[400]),
                  SizedBox(height: 8),
                  Text(
                    '이미지를 추가해주세요',
                    style: TextStyle(color: Colors.grey[600]),
                  ),
                ],
              ),
            ),
          )
        else
          GridView.builder(
            shrinkWrap: true,
            physics: NeverScrollableScrollPhysics(),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 8,
              mainAxisSpacing: 8,
              childAspectRatio: 1,
            ),
            itemCount: _images.length,
            itemBuilder: (context, index) {
              final image = _images[index];
              return Stack(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.grey[300]!),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.file(
                        File(image.localPath),
                        fit: BoxFit.cover,
                        width: double.infinity,
                        height: double.infinity,
                      ),
                    ),
                  ),
                  Positioned(
                    top: 4,
                    right: 4,
                    child: GestureDetector(
                      onTap: () {
                        setState(() {
                          _images.removeAt(index);
                        });
                      },
                      child: Container(
                        padding: EdgeInsets.all(4),
                        decoration: BoxDecoration(
                          color: Colors.red,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(Icons.close, size: 16, color: Colors.white),
                      ),
                    ),
                  ),
                  if (image.status == 'TEMPORARY')
                    Positioned(
                      bottom: 4,
                      left: 4,
                      child: Container(
                        padding:
                            EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.orange,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          '임시',
                          style: TextStyle(
                            fontSize: 10,
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
      ],
    );
  }

  Widget _buildContentSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '내용',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.grey[800],
          ),
        ),
        SizedBox(height: 8),
        Container(
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
            color: Colors.white,
          ),
          child: TextField(
            controller: _contentController,
            maxLines: null,
            minLines: 15,
            style: TextStyle(
              fontSize: 16,
              height: 1.6,
              color: Colors.grey[800],
            ),
            decoration: InputDecoration(
              hintText: '블로그 내용을 입력하세요...\n\n노션 스타일의 편집기를 사용해보세요!',
              hintStyle: TextStyle(color: Colors.grey[500]),
              border: InputBorder.none,
              contentPadding: EdgeInsets.all(16),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPreviewSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '미리보기',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.grey[800],
          ),
        ),
        SizedBox(height: 12),
        Container(
          width: double.infinity,
          padding: EdgeInsets.all(16),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
            color: Colors.grey[50],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_titleController.text.isNotEmpty) ...[
                Text(
                  _titleController.text,
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                SizedBox(height: 8),
              ],
              if (_tags.isNotEmpty) ...[
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: _tags
                      .map(
                        (tag) => Container(
                          padding: EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.blue[100],
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            tag,
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.blue[800],
                            ),
                          ),
                        ),
                      )
                      .toList(),
                ),
                SizedBox(height: 12),
              ],
              if (_contentController.text.isNotEmpty) ...[
                Text(
                  _contentController.text,
                  style: TextStyle(
                    fontSize: 16,
                    height: 1.6,
                    color: Colors.grey[800],
                  ),
                ),
                SizedBox(height: 12),
              ],
              if (_images.isNotEmpty) ...[
                Text(
                  '이미지 ${_images.length}개',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                    fontStyle: FontStyle.italic,
                  ),
                ),
              ],
              SizedBox(height: 12),
              Row(
                children: [
                  Container(
                    padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(_selectedStatus),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      _getStatusText(_selectedStatus),
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.white,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  SizedBox(width: 8),
                  Container(
                    padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.grey[200],
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      _getAccessLevelText(_selectedAccessLevel),
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey[700],
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'DRAFT':
        return Colors.orange;
      case 'PUBLISHED':
        return Colors.green;
      case 'ARCHIVED':
        return Colors.grey;
      default:
        return Colors.grey;
    }
  }

  String _getStatusText(String status) {
    switch (status) {
      case 'DRAFT':
        return '임시저장';
      case 'PUBLISHED':
        return '발행';
      case 'ARCHIVED':
        return '보관';
      default:
        return '알 수 없음';
    }
  }

  String _getAccessLevelText(String accessLevel) {
    switch (accessLevel) {
      case 'PUBLIC':
        return '공개';
      case 'PRIVATE':
        return '비공개';
      case 'SHARED':
        return '공유';
      case 'PASSWORD':
        return '비밀번호';
      default:
        return '알 수 없음';
    }
  }
}

class BlogImage {
  final int id;
  final String localPath;
  final String fileName;
  final int fileSize;
  final DateTime uploadTime;
  final int? serverImageId; // 서버에서 받은 이미지 ID
  final String? status; // TEMPORARY, CONFIRMED

  BlogImage({
    required this.id,
    required this.localPath,
    required this.fileName,
    required this.fileSize,
    required this.uploadTime,
    this.serverImageId,
    this.status,
  });
}
