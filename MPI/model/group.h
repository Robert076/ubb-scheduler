#pragma once
#include <string>
#include <vector>

class Group
{
private:
    std::string id;
    int size = 30; // Default size
    std::string language = "English";

    // List of subject names this group attends
    std::vector<std::string> subjects;

    // How many subgroups for seminars/labs?
    // 1 = Whole group
    // 2 = Half group (Semigroup)
    int seminarySplit = 1;
    int laboratorySplit = 1;

public:
    Group() {} // Default constructor
    Group(std::string id) : id(id) {}

    // --- Getters ---
    std::string getId() const { return id; }
    int getSize() const { return size; }
    std::string getLanguage() const { return language; }
    const std::vector<std::string> &getSubjects() const { return subjects; }
    int getSeminarySplit() const { return seminarySplit; }
    int getLaboratorySplit() const { return laboratorySplit; }

    // --- Setters (ADDED THESE TO FIX ERRORS) ---
    void setSize(int s) { size = s; }
    void setLanguage(const std::string &l) { language = l; }

    void addSubject(const std::string &subject)
    {
        subjects.push_back(subject);
    }

    void setSeminarySplit(int split) { seminarySplit = split; }
    void setLaboratorySplit(int split) { laboratorySplit = split; }
};